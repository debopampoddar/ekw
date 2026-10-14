package ekw.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory FactSink for tests.
 */
public final class CollectingSink implements Facts.FactSink {
    public final List<Facts.TypeFact> types = new ArrayList<>();
    public final List<Facts.MemberFact> members = new ArrayList<>();
    public final List<Facts.CallFact> calls = new ArrayList<>();

    @Override
    public void type(Facts.TypeFact t) {
        types.add(t);
    }

    @Override
    public void member(Facts.MemberFact m) {
        members.add(m);
    }

    @Override
    public void call(Facts.CallFact c) {
        calls.add(c);
    }

    @Override
    public void annotation(Facts.AnnotationFact a) {
    }

    @Override
    public void configKey(Facts.ConfigKeyFact k) {
    }

    @Override
    public void errorCode(Facts.ErrorCodeFact e) {
    }

    @Override
    public void spi(Facts.SpiFact s) {
    }

    @Override
    public void module(Facts.ModuleFact m) {
    }

    @Override
    public void dependency(Facts.DependencyFact d) {
    }
}
