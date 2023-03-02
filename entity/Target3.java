package entity;

import annotation.PropertyTransformer;

/**
 * @author Jinyi Wang
 * @date 2023/2/20 17:33
 */
public class Target3 {
    @PropertyTransformer
    private Inner inner;

    public static class Inner {
        private Inner(String inn) {
            this.inn = inn;
        }

        @PropertyTransformer({"inn", "innn"})
        private String inn;
    }
}
