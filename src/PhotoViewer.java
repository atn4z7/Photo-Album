import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PhotoViewer {
    // Main entry point
    public static void main(String[] args) throws SQLException {
        JFrame frame = new PhotoFrame();
        frame.setSize(600, 600);
        frame.setVisible(true);
    }
}

class PhotoFrame extends JFrame implements ActionListener {
    private JButton deleteBtn = null;
    private JButton saveBtn = null;
    private JButton addBtn = null;
    private JButton prevBtn = null;
    private JButton nextBtn = null;
    private JTextField IndexField = null;
    private JTextArea DescriptionArea = null;
    private JTextField DateField = null;
    private JLabel pictureCountLabel = null;
    private JLabel imageLbl = null;
    private Database data = null;
    private int currentIndex = 1;
    private JMenuItem exitMenuItem = null;
    private JMenuItem browseMenuItem = null;
    private JMenuItem maintainMenuItem = null;
    private boolean maintainmode;
    private Photo temp = null;
    private int currentTotal;

    class UpdateDisplayThread extends Thread {
        private Database data;

        public UpdateDisplayThread(Database Data) {
            this.data = Data;
        }

        @Override
        public void run() {
            try {
                if (currentTotal != 0) {
                    //go to database to retrieve photo and then update GUI in UI thread
                    System.out.println("getting data with index " + currentIndex);
                    temp = data.getPhoto(currentIndex);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                System.out.println("updating display");
                                updateDisplay();
                                System.out.println("setting null");
                                temp = null;
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    //no photo => display default screen
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            defaultDisplay();
                        }
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    class UploadPhotoThread extends Thread {
        private Database data;

        public UploadPhotoThread(Database Data) {
            this.data = Data;
        }

        @Override
        public void run() {
            try {
                //upload temp to database and update display
                temp = data.getTemp();
                data.uploadTemp();

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            System.out.println("updating display");
                            updateDisplay();
                            System.out.println("setting null");
                            temp = null;
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    class DeletePhotoThread extends Thread {
        private Database data;
        private int index;

        public DeletePhotoThread(Database Data, int Index) {
            this.data = Data;
            this.index = Index;
        }

        @Override
        public void run() {
            try {
                //delete photo from database and update display
                data.deletePhoto(index);
                UpdateDisplayThread update = new UpdateDisplayThread(data);
                update.start();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    class UpdatePhotoInfoThread extends Thread {
        private Database data;
        private int index;
        private String description, date;

        public UpdatePhotoInfoThread(Database Data, int Index, String Description, String Date) {
            this.data = Data;
            this.index = Index;
            this.description = Description;
            this.date = Date;
        }

        @Override
        public void run() {
            try {
                //update photo info on database
                data.updatePhotoInfo(index, description, date);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    class StoreTempThread extends Thread {
        private Database data;
        private Path path;
        private int index;

        public StoreTempThread(Database Data, Path Path, int Index) {
            this.data = Data;
            this.index = Index;
            this.path = Path;
        }

        @Override
        public void run() {
            //store temp photo locally
            data.storeTemp(path, index);
        }
    }

    public PhotoFrame() throws SQLException {
        //set title of the window
        super("Photo Album");
        data = new Database();
        currentTotal = data.numberOfPhotos();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.out.println("exit clicked");
                System.exit(0);
            }
        });

        Container contentPane = getContentPane();
        //create a menu
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // Create the 1st menu.
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menu);

        // Create an item for the first menu
        exitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitMenuItem.addActionListener(this);

        menu.add(exitMenuItem);
        // Create the 2nd menu.
        JMenu menu2 = new JMenu("View");
        menu2.setMnemonic(KeyEvent.VK_V);
        menuBar.add(menu2);

        // Create items for the 2nd menu
        browseMenuItem = new JMenuItem("Browse", KeyEvent.VK_B);
        browseMenuItem.addActionListener(this);
        maintainMenuItem = new JMenuItem("Maintain", KeyEvent.VK_M);
        maintainMenuItem.addActionListener(this);

        menu2.add(browseMenuItem);
        menu2.add(maintainMenuItem);

        //create a scrollPane that contains an image
        imageLbl = new JLabel("", SwingConstants.CENTER);
        imageLbl.setFont(imageLbl.getFont().deriveFont(32.0f));
        JScrollPane scrollPane = new JScrollPane(imageLbl);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        //create a box layout panel to contain description panel, date panel, buttons panel
        JPanel controlPane = new JPanel();
        controlPane.setLayout(new BoxLayout(controlPane, BoxLayout.PAGE_AXIS));

        //description panel
        JPanel descriptionPane = new JPanel();
        descriptionPane.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel descriptionLabel = new JLabel("Description:");
        DescriptionArea = new JTextArea(4, 20);
        DescriptionArea.setEditable(false);
        DescriptionArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                //System.out.println("Description removeUpdate");
                if (!saveBtn.isEnabled() && maintainmode) {
                    saveBtn.setEnabled(true);
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                //System.out.println("Description insertUpdate");
                if (!saveBtn.isEnabled() && maintainmode) {
                    saveBtn.setEnabled(true);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent arg0) {
                //System.out.println("changedUpdate");
            }
        });
        Border descriptionBorder = BorderFactory.createLineBorder(Color.DARK_GRAY, 1, false);
        DescriptionArea.setBorder(descriptionBorder);
        descriptionPane.add(descriptionLabel);
        descriptionPane.add(DescriptionArea);

        //jpanel to contain date panel and right button panel
        JPanel leftRightPane = new JPanel(new BorderLayout());

        //date panel
        JPanel datePane = new JPanel();
        datePane.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setPreferredSize(new Dimension(descriptionLabel.getPreferredSize().width, dateLabel.getPreferredSize().height));
        DateField = new JTextField("");
        DateField.setPreferredSize(new Dimension(DescriptionArea.getPreferredSize().width / 2, DateField.getPreferredSize().height));
        DateField.setEditable(false);
        DateField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                //System.out.println("Date removeUpdate");
                if (!saveBtn.isEnabled() && maintainmode) {
                    saveBtn.setEnabled(true);
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                //System.out.println("Date insertUpdate");
                if (!saveBtn.isEnabled() && maintainmode) {
                    saveBtn.setEnabled(true);
                }
            }

            @Override
            public void changedUpdate(DocumentEvent arg0) {
                //System.out.println("Date changedUpdate");
            }
        });
        datePane.add(dateLabel);
        datePane.add(DateField);

        //right button panel
        JPanel rightbuttonPane = new JPanel();
        deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(this);
        saveBtn = new JButton("Save Changes");
        saveBtn.addActionListener(this);
        addBtn = new JButton("Add");
        addBtn.addActionListener(this);
        deleteBtn.setVisible(false);
        saveBtn.setVisible(false);
        addBtn.setVisible(false);

        rightbuttonPane.add(deleteBtn);
        rightbuttonPane.add(saveBtn);
        rightbuttonPane.add(addBtn);

        leftRightPane.add(datePane, BorderLayout.WEST);
        leftRightPane.add(rightbuttonPane, BorderLayout.EAST);
        //buttons panel
        JPanel southButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        IndexField = new JTextField("", 4);
        pictureCountLabel = new JLabel("");
        prevBtn = new JButton("<Prev");
        prevBtn.addActionListener(this);
        nextBtn = new JButton("Next>");
        nextBtn.addActionListener(this);

        southButtonPanel.add(IndexField);
        southButtonPanel.add(pictureCountLabel);
        southButtonPanel.add(prevBtn);
        southButtonPanel.add(nextBtn);

        //adding panels
        controlPane.add(descriptionPane);
        controlPane.add(leftRightPane);
        controlPane.add(southButtonPanel);

        contentPane.add(controlPane, BorderLayout.SOUTH);

        //update photo
        //updateDisplay();
        updateBtnState();
        UpdateDisplayThread update = new UpdateDisplayThread(data);
        update.start();
    }

    //function to set what will be displayed when we have no image
    public synchronized void defaultDisplay() {
        imageLbl.setIcon(null);
        imageLbl.setText("Click Add to add image here");
        DescriptionArea.setText("Enter Description here!");
        DescriptionArea.setEditable(false);
        DateField.setText("Enter Date here!");
        DateField.setEditable(false);
        currentIndex = 0;
        IndexField.setText(Integer.toString(currentIndex));
        IndexField.setEditable(false);
        pictureCountLabel.setText("0");
        //display add,delete,save buttons
        deleteBtn.setVisible(true);
        saveBtn.setVisible(true);
        saveBtn.setEnabled(false);
        addBtn.setVisible(true);
        maintainmode = true;
        // no image -> nothing to delete
        deleteBtn.setEnabled(false);
    }

    //function to update the display based on the current index
    public synchronized void updateDisplay() throws SQLException {
        System.out.println("Updating display with current index " + currentIndex);
        imageLbl.setText("");
        imageLbl.setIcon(temp.getIMG());
        //update description, date, pic count
        DateField.setText(temp.getDate());
        DescriptionArea.setText(temp.getDescription());
        IndexField.setText(Integer.toString(currentIndex));
        pictureCountLabel.setText("of " + currentTotal);
        //disable save button every time we are done updating
        saveBtn.setEnabled(false);
    }

    //function to update the state of navigating buttons
    public void updateBtnState() throws SQLException {
        //if no photo or only 1 photo, disable both buttons
        if (currentTotal == 0 || currentTotal == 1) {
            nextBtn.setEnabled(false);
            prevBtn.setEnabled(false);
        } else { //if there are 2+ photos
            if (currentIndex == currentTotal) { //reaches the max => disable next, enable prev
                nextBtn.setEnabled(false);
                prevBtn.setEnabled(true);
            } else if (currentIndex == 1) {  //reaches the min => enable next, disable prev
                prevBtn.setEnabled(false);
                nextBtn.setEnabled(true);
            } else {
                nextBtn.setEnabled(true);
                prevBtn.setEnabled(true);
            }
        }
    }

    public void addpopup() throws SQLException {
        JFileChooser chooser = new JFileChooser("D:\\java\\photo viewer");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "JPG,GIF,PNG Images", "jpg", "gif", "png");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.println(chooser.getSelectedFile().getName() + " was selected!");
            //if there is no image and we first add a photo, currentIndex must be 1 so that
            //the new photo will have pictnum == 1
            if (currentIndex == 0) currentIndex = 1;

            File file = chooser.getSelectedFile();
            //get path of file
            Path path = Paths.get(file.getAbsolutePath());
            //display the new image
            DateField.setText("");
            DescriptionArea.setText("");
            ImageIcon image = new ImageIcon(file.getAbsolutePath());
            imageLbl.setText("");
            imageLbl.setIcon(image);

            // create a new thread to temporarily store the photo
            System.out.println("Storing temp photo with index " + currentIndex);
            StoreTempThread store = new StoreTempThread(data, path, currentIndex);
            store.start();

            //enable editing
            DescriptionArea.setEditable(true);
            DateField.setEditable(true);
            IndexField.setEditable(true);
            //disable navigating + adding functions until saving the changes
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            addBtn.setEnabled(false);
            //enable the delete button when we have 1+ photos
            if (!deleteBtn.isEnabled())
                deleteBtn.setEnabled(true);
            //enable save button so we can save the new photo
            if (!saveBtn.isEnabled())
                saveBtn.setEnabled(true);
        }
    }

    public void gotoMaintain(boolean bool) {
        deleteBtn.setVisible(bool);
        saveBtn.setVisible(bool);
        addBtn.setVisible(bool);
        DescriptionArea.setEditable(bool);
        DateField.setEditable(bool);
        maintainmode = bool;
    }

    //actions for the buttons
    public void actionPerformed(ActionEvent evt) {
        try {
            if (evt.getSource() == prevBtn) {
                System.out.println("Prev Button clicked");
                //decrease index by 1
                currentIndex--;
                //update the state of navigating buttons
                updateBtnState();
                //create a new thread to retrieve and display previous photo
                UpdateDisplayThread update = new UpdateDisplayThread(data);
                update.start();
            } else if (evt.getSource() == nextBtn) {
                System.out.println("Next Button clicked");
                //increase index by 1
                currentIndex++;
                //update the state of navigating buttons
                updateBtnState();
                //create a new thread to retrieve and display next photo
                UpdateDisplayThread update = new UpdateDisplayThread(data);
                update.start();
            } else if (evt.getSource() == deleteBtn) {
                System.out.println("delete clicked");
                if (data.hasTemp()) {
                    //delete temp photo
                    data.deleteTemp();
                    addBtn.setEnabled(true);
                } else {
                    if (currentTotal > 0) {
                        currentTotal--;
                        int DeleteIndex = currentIndex;
                        //reassign the index if it is out of range
                        if (currentIndex > currentTotal)
                            currentIndex = currentTotal;
                        //create a new thread to delete the photo and update display
                        DeletePhotoThread update = new DeletePhotoThread(data, DeleteIndex);
                        update.start();
                    }
                }
                updateBtnState();
            } else if (evt.getSource() == addBtn) {
                System.out.println("add clicked");
                addpopup();
            } else if (evt.getSource() == saveBtn) {
                System.out.println("save clicked");
                saveBtn.setEnabled(false);
                if (data.hasTemp()) {//if we have a temp photo
                    try {
                        //update info of temp photo before uploading to database
                        data.updateTempInfo(DescriptionArea.getText(), DateField.getText());
                        currentTotal++;
                        //create a new thread to upload temp photo to database and update display
                        UploadPhotoThread upload = new UploadPhotoThread(data);
                        upload.start();
                        //re-enable the add button
                        addBtn.setEnabled(true);
                        //update pre/next buttons state
                        updateBtnState();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                //create a new thread to update description + date of the on screen photo on database
                    UpdatePhotoInfoThread update = new UpdatePhotoInfoThread(data, currentIndex, DescriptionArea.getText(), DateField.getText());
                    update.start();
                }
            } else if (evt.getSource() == exitMenuItem) {
                System.out.println("exit clicked");
                System.exit(0);
            } else if (evt.getSource() == browseMenuItem) {
                System.out.println("browse clicked");
                gotoMaintain(false);
            } else if (evt.getSource() == maintainMenuItem) {
                System.out.println("maintain clicked");
                gotoMaintain(true);
            } else {
                System.out.println("Unexpected event: " + evt);
            }
        } catch (SQLException ex) {
            System.out.println("*** SQLException");

            while (ex != null) {
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("Message: " + ex.getMessage());
                System.out.println("Vendor: " + ex.getErrorCode());
                ex = ex.getNextException();
                System.out.println("");
            }
        }
    }
}



