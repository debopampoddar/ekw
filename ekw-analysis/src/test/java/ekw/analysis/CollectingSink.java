package ekw.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory FactSink for tests.
 */
public final class CollectingSink implements Facts.FactSink {
    public final List<TypeFact> types = new ArrayList<>();
    public final List<MemberFact> members = new ArrayList<>();
    public final List<CallFact> calls = new ArrayList<>();

    @Override
    public void type(TypeFact t) {
        types.add(t);
    }

    @Override
    public void member(MemberFact m) {
        members.add(m);
    }

    @Override
    public void call(CallFact c) {
        calls.add(c);
    }

    @Override
    public void annotation(AnnotationFact a) {
    }

    @Override
    public void configKey(ConfigKeyFact k) {
    }

    @Override
    public void errorCode(ErrorCodeFact e) {
    }

    @Override
    public void spi(SpiFact s) {
    }

    @Override
    public void module(ModuleFact m) {
    }

    @Override
    public void dependency(DependencyFact d) {
    }
}
