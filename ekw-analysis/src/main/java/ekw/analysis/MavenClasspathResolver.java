package ekw.analysis;


import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Builds a module's effective POM (parents, properties, dependency management; no plugins run) and downloads its
 * dependency jars from Maven Central into a cache folder. Only POMs and jars are read; nothing is executed.
 *
 * <p>Chapter 4 of the development guide.
 */
public final class MavenClasspathResolver
        implements Facts.ClasspathResolver, AutoCloseable {

    public static final RemoteRepository CENTRAL =
            new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();

    private final RepositorySystem system; //The download engine
    private final RepositorySystemSession.CloseableSession session; //Settings for this resolver: the jar cache folder
    private final Map<String, Path> reactor; //The repository’s own POMs, from reactorPoms
    private final Map<Path, Model> models = new HashMap<>(); //A cache of effective models, so each POM is built once
    private static final String GROUP_ID_ARTIFACT_ID_FORMAT = "%s:%s";
    private static final String GROUP_ID_ARTIFACT_ID_VERSION_FORMAT = "%s:%s:%s";
    /**
     * @param localRepository the jar cache, e.g. {@code <kb>/.cache/m2}
     * @param repositoryRoot  the checkout; its own POMs are read from disk, never downloaded
     */
    public MavenClasspathResolver(Path localRepository, Path repositoryRoot) {
        this.system = new RepositorySystemSupplier().get();
        this.session = new SessionBuilderSupplier(system)
                .get()
                .withLocalRepositoryBaseDirectories(localRepository)
                .setSystemProperties(System.getProperties())
                .build();
        this.reactor = reactorPoms(repositoryRoot);
    }

    /**
     * groupId:artifactId -> pom.xml for every POM in the checkout (build output and test resources excluded).
     */
    public static Map<String, Path> reactorPoms(Path repositoryRoot) {
        Map<String, Path> out = new TreeMap<>();
        try (Stream<Path> files = Files.walk(repositoryRoot)) {
            for (Path pom : files.filter(p -> p.getFileName().toString().equals("pom.xml"))
                                 .toList()) {
                String relativePaths = repositoryRoot.relativize(pom).toString().replace('\\', '/');
                if (relativePaths.contains("/target/")
                        || relativePaths.contains("/src/test/")
                        || relativePaths.contains("/src/it/"))
                    continue;
                try (Reader reader = Files.newBufferedReader(pom)) {
                    Model raw = new MavenXpp3Reader().read(reader);
                    String groupId = null != raw.getGroupId() ? raw.getGroupId()
                                                              : null != raw.getParent()
                                                                ? raw.getParent().getGroupId()
                                                                : null;
                    String artifactId = raw.getArtifactId();
                    if (null != groupId && null != artifactId) {
                        out.putIfAbsent(GROUP_ID_ARTIFACT_ID_FORMAT.formatted(groupId, artifactId),
                                pom);
                    }
                } catch (XmlPullParserException e) {
                    ;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    /**
     * The effective model of a pom.xml.
     */
    public Model effectiveModel(Path pom) {
        Path key = pom.toAbsolutePath().normalize();
        Model cached = models.get(key);
        if (cached != null) {
            return cached;
        }

        DefaultModelBuildingRequest defaultModelBuildingRequest = new DefaultModelBuildingRequest();
        defaultModelBuildingRequest.setPomFile(pom.toFile());
        defaultModelBuildingRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        defaultModelBuildingRequest.setProcessPlugins(false);
        defaultModelBuildingRequest.setTwoPhaseBuilding(false);
        defaultModelBuildingRequest.setSystemProperties(System.getProperties());
        defaultModelBuildingRequest.setUserProperties(new Properties());
        defaultModelBuildingRequest.setModelResolver(new CentralModelResolver());

        try {
            Model model = new DefaultModelBuilderFactory().newInstance()
                                            .build(defaultModelBuildingRequest)
                                            .getEffectiveModel();
            models.put(key, model);
            return model;
        } catch (ModelBuildingException e) {
            throw new IllegalStateException("cannot build effective POM " + pom + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Facts.Classpath resolve(Facts.ModuleSpec module) {
        Model model = effectiveModel(module.root().resolve("pom.xml"));
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRepositories(List.of(CENTRAL));
        List<String> unresolved = new ArrayList<>();
        Map<String, org.apache.maven.model.Dependency> external = new LinkedHashMap<>();
        collectExternal(model, true, external, unresolved, new HashSet<>());
        external.values().forEach(d -> collectRequest.addDependency(toAether(d)));
        if (model.getDependencyManagement() != null) {
            for (org.apache.maven.model.Dependency d : model.getDependencyManagement().getDependencies()) {
                if (!"import".equals(d.getScope()) && !reactor.containsKey(key(d)))
                    collectRequest.addManagedDependency(toAether(d));
            }
        }
        DependencyRequest request = new DependencyRequest(collectRequest,
                DependencyFilterUtils.classpathFilter("compile", "provided", "runtime", "test"));
        DependencyResult result;
        try {
            result = system.resolveDependencies(session, request);
        } catch (DependencyResolutionException e) {
            result = e.getResult();                      // keep what resolved; JDT recovers bindings for the rest
            if (result == null) throw new IllegalStateException(e);
        }
        Set<Path> jars = new LinkedHashSet<>();
        for (ArtifactResult r : result.getArtifactResults()) {
            if (r.isResolved() && r.getArtifact().getPath() != null) {
                jars.add(r.getArtifact().getPath());
            } else {
                unresolved.add(r.getRequest().getArtifact().toString());
            }
        }
        return new Facts.Classpath(List.copyOf(jars), List.copyOf(unresolved));
    }

    /**
     * Dependencies to download: the module's own non-reactor dependencies (all scopes), plus the compile and runtime
     * dependencies of the reactor modules it depends on, recursively (their sources are on the source path, but the
     * jars they need are not).
     */
    private void collectExternal(Model model, boolean direct, Map<String, Dependency> out,
                                 List<String> unresolved, Set<String> visited) {

        for(Dependency dependency : model.getDependencies()) {
            String scope = dependency.getScope() == null ? "compile" : dependency.getScope();
            if (!direct && !(scope.equals("compile") || scope.equals("runtime")))
                continue;
            Path siblingPom = reactor.get(key(dependency));
            if (siblingPom != null) {
                if (visited.add(key(dependency)))
                    collectExternal(effectiveModel(siblingPom),
                            false,
                            out,
                            unresolved,
                            visited);
                continue;
            }
            if (scope.equals("system") || scope.equals("import")) {
                unresolved.add(coordinates(dependency) + " (scope " + scope + ")");
                continue;
            }
            out.putIfAbsent(key(dependency) + ":"
                            + (dependency.getClassifier() == null ? "" : dependency.getClassifier()),
                    dependency);
        }
    }

    private static String key(Dependency d) {
        return GROUP_ID_ARTIFACT_ID_FORMAT.formatted(d.getGroupId(), d.getArtifactId());
    }

    private static String coordinates(org.apache.maven.model.Dependency d) {
        return GROUP_ID_ARTIFACT_ID_VERSION_FORMAT.formatted(d.getGroupId(),
                d.getArtifactId(),
                d.getVersion());
    }

    private org.eclipse.aether.graph.Dependency toAether(org.apache.maven.model.Dependency dependency) {
        ArtifactType artifactType = session.getArtifactTypeRegistry()
                       .get(null == dependency.getType() ? "jar" : dependency.getType());
        String extension = artifactType != null ? artifactType.getExtension() : dependency.getType();
        String classifier = dependency.getClassifier() != null
                                    && !dependency.getClassifier().isEmpty()
                            ? dependency.getClassifier() : artifactType != null
                                                           ? artifactType.getClassifier() : "";
        List<Exclusion> exclusions = dependency.getExclusions().stream()
                                               .map(x -> new Exclusion(x.getGroupId(),
                                                       x.getArtifactId(),
                                                       "*",
                                                       "*"))
                                               .toList();
        org.eclipse.aether.graph.Dependency newDependency
                = new org.eclipse.aether.graph.Dependency(new DefaultArtifact(dependency.getGroupId(),
                dependency.getArtifactId(), classifier, extension,
                dependency.getVersion()), dependency.getScope() == null ? "compile" : dependency.getScope(), dependency.isOptional(), exclusions);

        return newDependency;
    }

    @Override
    public void close() {
        session.close();
        system.close();
    }

    @SuppressWarnings("deprecation")       // ModelResolver's own signatures use ModelSource, deprecated in Maven 3.9
    private final class CentralModelResolver implements ModelResolver {

        /**
         * Tries to resolve the POM for the specified coordinates.
         *
         * @param groupId    The group identifier of the POM, must not be {@code null}.
         * @param artifactId The artifact identifier of the POM, must not be {@code null}.
         * @param version    The version of the POM, must not be {@code null}.
         * @return The source of the requested POM, never {@code null}.
         * @throws UnresolvableModelException If the POM could not be resolved from any configured repository.
         */
        @Override
        public ModelSource resolveModel(String groupId, String artifactId, String version)
                throws UnresolvableModelException {
            Path localPath = reactor.get(GROUP_ID_ARTIFACT_ID_FORMAT
                    .formatted(groupId, artifactId));
            if (localPath != null)
                return new FileModelSource(localPath.toFile());

            try {
                ArtifactResult result = system.resolveArtifact(session, new ArtifactRequest(
                        new DefaultArtifact(groupId, artifactId, "", "pom", version), List.of(CENTRAL), null));
                return new FileModelSource(result.getArtifact().getPath().toFile());
            } catch (ArtifactResolutionException e) {
                throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
            }
        }

        /**
         * Tries to resolve the POM for the specified parent coordinates possibly updating {@code parent}.
         * <p>
         * Unlike the {@link #resolveModel(String, String, String)} method, this method
         * supports version ranges and updates the given {@code parent} instance to match the returned {@code ModelSource}.
         * If {@code parent} declares a version range, the version corresponding to the returned {@code ModelSource} will
         * be set on the given {@code parent}.
         * </p>
         *
         * @param parent The parent coordinates to resolve, must not be {@code null}.
         * @return The source of the requested POM, never {@code null}.
         * @throws UnresolvableModelException If the POM could not be resolved from any configured repository.
         * @see Parent#clone()
         * @since 3.2.2
         */
        @Override
        public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
            return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        }

        /**
         * Tries to resolve the POM for the specified dependency coordinates possibly updating {@code dependency}.
         * <p>
         * Unlike the {@link #resolveModel(String, String, String)} method, this method
         * supports version ranges and updates the given {@code dependency} instance to match the returned
         * {@code ModelSource}. If {@code dependency} declares a version range, the version corresponding to the returned
         * {@code ModelSource} will be set on the given {@code dependency}.
         * </p>
         *
         * @param dependency The dependency coordinates to resolve, must not be {@code null}.
         * @return The source of the requested POM, never {@code null}.
         * @throws UnresolvableModelException If the POM could not be resolved from any configured repository.
         * @see Dependency#clone()
         * @since 3.5.0
         */
        @Override
        public ModelSource resolveModel(org.apache.maven.model.Dependency dependency)
                throws UnresolvableModelException {
            return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        }

        /**
         * Adds a repository to use for subsequent resolution requests. The order in which repositories are added matters,
         * repositories that were added first should also be searched first. When multiple repositories with the same
         * identifier are added, only the first repository being added will be used.
         *
         * @param repository The repository to add to the internal search chain, must not be {@code null}.
         * @throws InvalidRepositoryException If the repository could not be added (e.g. due to invalid URL or layout).
         */
        @Override
        public void addRepository(Repository repository) throws InvalidRepositoryException {

        }

        /**
         * Adds a repository to use for subsequent resolution requests. The order in which repositories are added matters,
         * repositories that were added first should also be searched first. When multiple repositories with the same
         * identifier are added, then the value of the replace argument is determines the behaviour.
         * <p>
         * If replace is false than any existing repository with the same Id will remain in use. If replace
         * is true the new repository replaces the original.
         *
         * @param repository The repository to add to the internal search chain, must not be {@code null}.
         * @param replace
         * @throws InvalidRepositoryException If the repository could not be added (e.g. due to invalid URL or layout).
         */
        @Override
        public void addRepository(Repository repository, boolean replace)
                throws InvalidRepositoryException {

        }

        /**
         * Clones this resolver for usage in a forked resolution process. In general, implementors need not provide a deep
         * clone. The only requirement is that invocations of {@link #addRepository(Repository)} on the clone do not affect
         * the state of the original resolver and vice versa.
         *
         * @return The cloned resolver, never {@code null}.
         */
        @Override
        public ModelResolver newCopy() {
            return this;
        }
    }
}
