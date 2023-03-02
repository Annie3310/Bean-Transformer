package entity;

import annotation.PropertyTransformer;

import java.util.Objects;

/**
 * @author Jinyi Wang
 * @date 2023/2/20 14:00
 */
public class Source1 {
    @PropertyTransformer("addr")
    private String addr;

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
        Source1 source1 = (Source1) o;
        return Objects.equals(addr, source1.addr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addr);
    }

    @Override
    public String toString() {
        return "Source1{" +
                "name='" + addr + '\'' +
                '}';
    }
}
