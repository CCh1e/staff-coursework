package ua.sumdu.staff.model;

import java.util.Objects;

/**
 * Підрозділ підприємства (таблиця departments).
 */
public class Department {

    private int id;
    private String name;
    private String location;
    private String phone;

    public Department() {
    }

    public Department(int id, String name, String location, String phone) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.phone = phone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** Чи збережено підрозділ у базі (новий об'єкт має id = 0). */
    public boolean isNew() {
        return id == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Department other)) {
            return false;
        }
        return id != 0 && id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /** Використовується у випадних списках інтерфейсу. */
    @Override
    public String toString() {
        return name;
    }
}
