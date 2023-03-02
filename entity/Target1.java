package entity;

import annotation.PropertyTransformer;

import java.util.Objects;

/**
 * test t1
 *
 * @author Jinyi Wang
 * @date 2023/2/17 17:57
 */
public class Target1 {
    @PropertyTransformer("n")
    private String name;

    @PropertyTransformer("a")
    private int age;
//    @PropertyTransformer({"inn", "innn"})
    @PropertyTransformer
    private Source.Inner inn;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Source.Inner getInn() {
        return inn;
    }

    public void setInn(Source.Inner inn) {
        this.inn = inn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Target1 target1 = (Target1) o;
        return age == target1.age && Objects.equals(name, target1.name) && Objects.equals(inn, target1.inn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age, inn);
    }

    @Override
    public String toString() {
        return "Target1{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", inn=" + inn +
                '}';
    }
}
