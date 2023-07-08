import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Universal Android Debloater GUI (2023)
 * 
 */
public class Debloat extends JFrame implements ActionListener {
  public static final String ADB_BASE_URL = "./src/adb";
  public static final String ADB_WINDOWS_URL = "/windows/adb.exe ";
  public static final String ADB_MACOS_URL = "/macos/adb ";
  public static final String ADB_LINUX_URL = "/linux/adb ";
  public static final String ADB_START_SERVER = "start-server";
  public static final String ADB_KILL_SERVER = "kill-server";
  public static final int ADB_SERVER_REFRESH_MILSECOND = 5000;
  public static final String ADB_HAS_DEVICE = "devices -l";
  public static final String ADB_GET_DEVICE_MANUFACTURER = "shell getprop ro.product.manufacturer";
  public static final String ADB_GET_DEVICE_MODEL = "shell getprop ro.product.model";
  public static final String ADB_GET_PACKAGES = "shell pm list packages";
  public static final String ADB_ENABLE_PACKAGE = "shell pm enable --user 0 ";
  public static final String ADB_DISABLE_PACKAGE = "shell pm disable-user --user 0 ";
  public static final String ADB_FORCE_DELETE_PACKAGE = "shell pm clear --user 0 ";
  public static final String TITLE = "Debloat";
  public static final Dimension WINDOW_SIZE = new Dimension(500, 800);
  public static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 14);
  public static final String DEFAULT_DEVICE_NAME = "Device: Not Found";
  public static final String DEFAULT_STATUS_BAR = "Searching for Devices...";
  public static final int PANE_CENTER_INSET = 15;
  public static final int PANE_SOUTH_INSET = 10;
  public static final int BTN_MARGIN = 5;
  public static final String[] BTN_NAMES = { "Select All", "Unselect All", "Disable", "Enable" };
  private JPanel pnlDeviceName;
  private JPanel pnlCenterContainer;
  private JPanel pnlPackages;
  private JPanel pnlSouthContainer;
  private JPanel pnlButtons;
  private JPanel pnlStatus;
  private JLabel lblDeviceName;
  private JLabel lblStatus;
  private HashMap<JCheckBox, Boolean> chkPackages;

  /**
   * Constructor that creates window
   *
   * @date 2022
   */
  public Debloat() {
    super(TITLE);
    setLayout(new BorderLayout());

    createDeviceNamePane();
    createPackagesPane();
    createButtonsPane();
    createStatusBar();

    adb(ADB_START_SERVER);

    chkPackages = new HashMap<>();

    // Detect devices
    Timer t = new Timer();
    t.schedule(new TimerTask() {
      @Override
      public void run() {
        if (adb(ADB_HAS_DEVICE).size() > 2) {
          lblStatus.setText("Device connected!");
          lblDeviceName
              .setText("Device: " + adb(ADB_GET_DEVICE_MANUFACTURER).get(0) + " " + adb(ADB_GET_DEVICE_MODEL).get(0));
          if (chkPackages.size() == 0) {
            for (String pkg : adb(ADB_GET_PACKAGES)) {
              // Remove 'package:' prefix from package names
              JCheckBox chk = new JCheckBox(pkg.substring(pkg.lastIndexOf(':') + 1));
              chk.setFont(DEFAULT_FONT);
              chk.setOpaque(false);
              pnlPackages.add(chk);
              chkPackages.put(chk, false);
              pack();
            }
          }
        } else {
          // If device is disconnected
          lblDeviceName.setText(DEFAULT_DEVICE_NAME);
          lblStatus.setText(DEFAULT_STATUS_BAR);
          pnlPackages.removeAll();
          chkPackages.clear();
        }
      }
      // Ensure device is connected
    }, 0, ADB_SERVER_REFRESH_MILSECOND);

    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setPreferredSize(WINDOW_SIZE);
    pack();
    setLocationRelativeTo(null);
    setVisible(true);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        dispose();
        t.cancel();
        t.purge();
        adb(ADB_KILL_SERVER);
      }
    });
  }

  /**
   * Execute adb shell command
   * 
   * @param cmd adb shell command to execute
   * @return command output line by line
   */
  public static ArrayList<String> adb(String cmd) {
    Process p;
    String adbcmd = "";
    // Check if OS is Windows
    try {
      p = Runtime.getRuntime().exec(ADB_BASE_URL + ADB_WINDOWS_URL + cmd);
      if (p.waitFor() != 0) {
        throw new IOException();
      }
      adbcmd = ADB_BASE_URL + ADB_WINDOWS_URL;
    } catch (Exception e) {
    }
    // Check if OS is Mac OS
    try {
      p = Runtime.getRuntime().exec(ADB_BASE_URL + ADB_MACOS_URL + cmd);
      if (p.waitFor() != 0) {
        throw new IOException();
      }
      adbcmd = ADB_BASE_URL + ADB_MACOS_URL;
    } catch (Exception e) {
    }
    // Check if OS is Linux
    try {
      p = Runtime.getRuntime().exec(ADB_BASE_URL + ADB_LINUX_URL + cmd);
      if (p.waitFor() != 0) {
        throw new IOException();
      }
      adbcmd = ADB_BASE_URL + ADB_LINUX_URL;
    } catch (Exception e) {
    }

    // Read outputs from adb command
    ArrayList<String> outputs = new ArrayList<>();
    try (InputStream is = Runtime.getRuntime().exec(adbcmd + cmd).getInputStream();
        Scanner s = new Scanner(is).useDelimiter("\\n")) {
      while (s.hasNext()) {
        outputs.add(s.next());
      }
      return outputs;
    } catch (IOException e) {
    }

    return new ArrayList<>(Arrays.asList("Device is not configured correctly."));
  }

  /**
   * Create a panel that contains device name
   * 
   */
  protected void createDeviceNamePane() {
    pnlDeviceName = new JPanel();
    lblDeviceName = new JLabel(DEFAULT_DEVICE_NAME);
    lblDeviceName.setFont(DEFAULT_FONT);
    pnlDeviceName.add(lblDeviceName);
    add(BorderLayout.NORTH, pnlDeviceName);
  }

  /**
   * Create a panel that contains a list of package names
   * 
   */
  protected void createPackagesPane() {
    pnlCenterContainer = new JPanel();
    pnlCenterContainer.setBorder(BorderFactory.createCompoundBorder(
        new EmptyBorder(PANE_CENTER_INSET, PANE_CENTER_INSET, PANE_CENTER_INSET, PANE_CENTER_INSET),
        new LineBorder(Color.LIGHT_GRAY)));
    pnlCenterContainer.setLayout(new GridLayout(0, 1));
    add(BorderLayout.CENTER, new JScrollPane(pnlCenterContainer));

    pnlPackages = new JPanel();
    pnlPackages.setBackground(Color.WHITE);
    pnlPackages.setLayout(new GridLayout(0, 1));
    pnlCenterContainer.add(pnlPackages);
  }

  /**
   * Create a panel that contains various buttons
   * 
   */
  protected void createButtonsPane() {
    pnlSouthContainer = new JPanel();
    pnlSouthContainer.setLayout(new BoxLayout(pnlSouthContainer, BoxLayout.Y_AXIS));
    pnlButtons = new JPanel();
    pnlButtons.setLayout(new GridLayout(0, BTN_NAMES.length / 2, BTN_MARGIN, BTN_MARGIN));
    pnlSouthContainer.add(pnlButtons);
    pnlButtons.setBorder(new EmptyBorder(PANE_SOUTH_INSET, PANE_SOUTH_INSET, PANE_SOUTH_INSET, PANE_SOUTH_INSET));

    for (int i = 0; i < BTN_NAMES.length; i++) {
      JButton btn = new JButton(BTN_NAMES[i]);
      btn.setFont(DEFAULT_FONT);
      btn.addActionListener(this);
      pnlButtons.add(btn);
    }
  }

  /**
   * Create a panel that contains the status bar
   * 
   */
  protected void createStatusBar() {
    pnlStatus = new JPanel();
    pnlStatus.setBorder(new BevelBorder(BevelBorder.LOWERED));
    pnlSouthContainer.add(pnlStatus);
    pnlStatus.setMaximumSize(getMaximumSize());
    pnlStatus.setLayout(new BoxLayout(pnlStatus, BoxLayout.X_AXIS));

    lblStatus = new JLabel(DEFAULT_STATUS_BAR);
    lblStatus.setFont(DEFAULT_FONT);
    pnlStatus.add(lblStatus);
    add(BorderLayout.SOUTH, pnlSouthContainer);
  }

  /**
   * Invoked when an action event occurs on a component
   * 
   * @param ev received action event from component
   */
  @Override
  public void actionPerformed(ActionEvent ev) {
    // Get name of the clicked button
    String source = ev.getSource().toString();

    int pkgCount = 1;
    for (Map.Entry<JCheckBox, Boolean> entry : chkPackages.entrySet()) {
      JCheckBox chk = entry.getKey();
      // Determine which button is clicked using button name
      if (source.contains("text=Select All")) {
        chk.setSelected(true);
      } else if (source.contains("text=Unselect All")) {
        chk.setSelected(false);
      } else if (source.contains("text=Enable")) {
        if (chk.isSelected()) {
          try {
            adb(ADB_ENABLE_PACKAGE + chk.getText());
            lblStatus.setText("Enabled " + pkgCount++ + " app(s) successfully.");
          } catch (Exception e) {
            lblStatus.setText("Failed enabling " + pkgCount + " app(s).");
          }
        }
      } else if (source.contains("text=Disable")) {
        if (chk.isSelected()) {
          try {
            adb(ADB_DISABLE_PACKAGE + chk.getText());
            lblStatus.setText("Disabled " + pkgCount++ + " app(s) successfully.");
          } catch (Exception e) {
            try {
              adb(ADB_FORCE_DELETE_PACKAGE + chk.getText());
              lblStatus.setText("Disabled " + pkgCount + " app(s) successfully.");
            } catch (Exception ex) {
              lblStatus.setText("Failed disabling " + pkgCount + " app(s).");
            }
          }
        }
      }
    }
  }

  /**
   * Main
   * 
   * @param args The command line arguments
   **/
  public static void main(String[] args) {
    new Debloat();
  }
}
