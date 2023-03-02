package entity;

import annotation.PropertyTransformer;

import java.util.List;
import java.util.Objects;

/**
 * test t2
 *
 * @author Jinyi Wang
 * @date 2023/2/17 17:57
 */
public class Target2 {

    @PropertyTransformer("p")
    private List<String> dogs;

    @PropertyTransformer("addr")
    private String addr;

    public List<String> getDogs() {
        return dogs;
    }

    public void setDogs(List<String> dogs) {
        this.dogs = dogs;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Target2 target2 = (Target2) o;
        return Objects.equals(dogs, target2.dogs) && Objects.equals(addr, target2.addr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dogs, addr);
    }

    @Override
    public String toString() {
        return "Target2{" +
                "dogs=" + dogs +
                ", addr='" + addr + '\'' +
                '}';
    }
}
