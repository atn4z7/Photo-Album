import javax.swing.*;

class Photo {
    private String Description;
    private String Date;
    private int id;
    private byte[] photo;

    public Photo(byte[] data, String description, String date) {
        photo = data;
        Description = description;
        Date = date;
    }

    public Photo(byte[] data) {
        photo = data;
    }

    public String getDescription() {
        return Description;
    }

    public String getDate() {
        return Date;
    }

    public int getID() {
        return id;
    }

    public byte[] getData() {
        return photo;
    }

    public ImageIcon getIMG() {
        return new ImageIcon(photo);
    }

    public void setDescription(String description) {
        Description = description;
    }

    public void setDate(String date) {
        Date = date;
    }

    public void setID(int ID) {
        id = ID;
    }
}