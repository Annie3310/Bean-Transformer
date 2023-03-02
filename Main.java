import entity.Source;
import entity.Source1;
import entity.Target1;
import entity.Target2;

import java.util.*;

/**
* 主类
* 
* @author Jinyi Wang
* @date 2023/2/17 15:37
*/
public class Main {
    public static void main(String[] args) {
        Source source = new Source();
        source.setAge(1);
        source.setName("a");
        source.setPets(Arrays.asList("d1", "d2"));
        Source.Inner inner = new Source.Inner();
        inner.setInn("in1");
        source.setInner(inner);

        Source1 source1 = new Source1();
        source1.setAddr("b");

        Target1 target1 = new Target1();
        Target2 target2 = new Target2();
        BeanTransformer.process(Arrays.asList(source, source1), target1, target2);
//        BeanTransformer.process(Collections.singletonList(source), target1, target2);
        System.out.println(target1);
        System.out.println(target2);

        Source source2 = new Source();
        source2.setPets(Arrays.asList("d1", "d2"));
        Source source3 = new Source();
        source3.setPets(source2.getPets());
    }
}
