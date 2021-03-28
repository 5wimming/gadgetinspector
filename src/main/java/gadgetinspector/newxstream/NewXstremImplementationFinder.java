package gadgetinspector.newxstream;

import gadgetinspector.ImplementationFinder;
import gadgetinspector.SerializableDecider;
import gadgetinspector.data.MethodReference;

import java.util.HashSet;
import java.util.Set;

public class NewXstremImplementationFinder implements ImplementationFinder{
    private final SerializableDecider serializableDecider;
    public NewXstremImplementationFinder(SerializableDecider serializableDecider){
        this.serializableDecider = serializableDecider;
    }
    @Override
    public Set<MethodReference.Handle> getImplementations(MethodReference.Handle target) {
        Set<MethodReference.Handle> allImpls = new HashSet<>();

        if (Boolean.TRUE.equals(serializableDecider.apply(target.getClassReference()))) {
            allImpls.add(target);
        }

        return allImpls;
    }
}
