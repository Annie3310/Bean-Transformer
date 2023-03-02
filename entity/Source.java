package entity;

import annotation.PropertyTransformer;

import java.util.List;
import java.util.Objects;

/**
 * test source
 *
 * @author Jinyi Wang
 * @date 2023/2/17 17:55
 */
public class Source {
    @PropertyTransformer("n")
    private String name;
    @PropertyTransformer("a")
    private Integer age;
    @PropertyTransformer("p")
    private List<String> pets;
    @PropertyTransformer
    private Inner inner;


    public static class Inner{
        @PropertyTransformer({"inn", "innn"})
        private String inn;

        public String getInn() {
            return inn;
        }

        public void setInn(String inn) {
            this.inn = inn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Inner inner = (Inner) o;
            return Objects.equals(inn, inner.inn);
        }

        @Override
        public int hashCode() {
            return Objects.hash(inn);
        }

        @Override
        public String toString() {
            return "Inner{" +
                    "in='" + inn + '\'' +
                    '}';
        }
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public List<String> getPets() {
        return pets;
    }

    public void setPets(List<String> pets) {
        this.pets = pets;
    }

    public Inner getInner() {
        return inner;
    }

    public void setInner(Inner inner) {
        this.inner = inner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Source source = (Source) o;
        return Objects.equals(name, source.name) && Objects.equals(age, source.age) && Objects.equals(pets, source.pets) && Objects.equals(inner, source.inner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age, pets, inner);
    }

    @Override
    public String toString() {
        return "Source{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", pets=" + pets +
                ", inner=" + inner +
                '}';
    }
}
