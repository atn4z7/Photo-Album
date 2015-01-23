import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.io.*;


class Database {
    //a temp photo to store the just-added photo before uploading it to database
    private Photo tempPhoto;
    public Connection con;
    public Statement stmt;

    public Database() {
        //set up the connection to database (the url, username and password for the database are specified here)
        String url = "jdbc:"+"url";
        String userID = "username";
        String password = "password";

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (java.lang.ClassNotFoundException e) {
            System.out.println(e);
            System.exit(0);
        }

        try {
            con = DriverManager.getConnection(url, userID, password);
            stmt = con.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int numberOfPhotos() throws SQLException {
        String sqlCmd = "SELECT COUNT(*) AS total FROM photos";
        ResultSet rs = stmt.executeQuery(sqlCmd);
        boolean more = rs.next();
        int total = rs.getInt("total");
        return total;
    }
    //function to retrieve a photo based on a given index from database
    public Photo getPhoto(int index) throws SQLException {
        System.out.println("Geting photo with index " + index);
        String date = null;
        String description = null;
        byte[] data = null;
        String sqlCmd = "SELECT date, description, photo FROM photos WHERE pictnum = " + (index);
        ResultSet rs = stmt.executeQuery(sqlCmd);
        boolean more = rs.next();
        if (more) {
            date = rs.getString(1);
            description = rs.getString(2);
            data = rs.getBytes(3);
        }
        Photo result = new Photo(data);
        result.setDate(date);
        result.setDescription(description);
        return result;
    }
    //function to assign data to the temp photo
    public void storeTemp(Path path, int index) {
        byte[] data = null;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tempPhoto = new Photo(data);
        tempPhoto.setID(index);
    }

    public boolean hasTemp() {
        if (tempPhoto == null) return false;
        else return true;
    }
    //function to upload the temp photo to database
    //it will first update index of old photos and then upload the temp photo
    public void uploadTemp() throws SQLException {
        PreparedStatement pstmt = null;
        System.out.println("Uploading temp photo with index " + tempPhoto.getID());
        String updatecmd = "UPDATE photos " +
                "SET pictnum = pictnum + 1 " +
                " WHERE pictnum >= " + tempPhoto.getID();
        // Format of table is: pictnum, date, description, photo
        String sql = "insert into photos(pictnum,date,description,photo) values (?, ?, ?, ?)";
        pstmt = con.prepareStatement(sql);
        pstmt.setInt(1, tempPhoto.getID()); // pictnum
        pstmt.setString(2, tempPhoto.getDate()); // date
        pstmt.setString(3, tempPhoto.getDescription()); // date
        pstmt.setBinaryStream(4, new ByteArrayInputStream(tempPhoto.getData()), tempPhoto.getData().length);
        try {
            stmt.executeUpdate(updatecmd);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        pstmt.close();
        //clear out temp
        tempPhoto = null;
    }
    //function to delete a photo from database based on a given index
    public void deletePhoto(int index) throws SQLException {
        System.out.println("Deleting photo with index " + index);
        //delete the photo with the given index from database
        String command1 = "DELETE from photos " +
                " WHERE pictnum = " + index;
        //update index of all photos after the deleted photo
        String command2 = "UPDATE photos " +
                "SET pictnum = pictnum - 1 " +
                " WHERE pictnum > " + index;
        stmt.executeUpdate(command1);
        stmt.executeUpdate(command2);
    }
    //function to update the info of a photo based on a given index from database
    public void updatePhotoInfo(int index, String description, String date) throws SQLException {
        System.out.println("Updating photo with index " + index);
        String command = "UPDATE photos " +
                "SET date = " + date + ", description = \"" + description +
                "\" WHERE pictnum = " + index;
        stmt.executeUpdate(command);
    }
    //function to get Temp Photo
    public Photo getTemp(){return tempPhoto;}
    //function to delete temp photo
    public void deleteTemp() {
        tempPhoto = null;
    }
    // function to update info of the temp photo
    public void updateTempInfo(String description, String date) {
        tempPhoto.setDescription(description);
        tempPhoto.setDate(date);
    }
}