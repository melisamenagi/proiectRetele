package ro.ase.retele;

public class User implements Runnable {

    private String name;

    public User(String name) {
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public void run() {

    }
}
