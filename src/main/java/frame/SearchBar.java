package frame;

import com.sun.awt.AWTUtilities;
import getIcon.GetIcon;
import main.MainClass;
import pinyin.PinYinConverter;
import search.Search;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static main.MainClass.mainExit;


public class SearchBar {
    private static SearchBar searchBarInstance = new SearchBar();
    private JFrame searchBar = new JFrame();
    private Container panel;
    private CopyOnWriteArrayList<String> listResult = new CopyOnWriteArrayList<>();
    private JLabel label1 = new JLabel();
    private JLabel label2 = new JLabel();
    private JLabel label3 = new JLabel();
    private JLabel label4 = new JLabel();
    private boolean isOpenLastFolderPressed = false;
    private int labelCount = 0;
    private JTextField textField;
    private Search search = Search.getInstance();
    private Color labelColor = new Color(255, 152, 104, 255);
    private Color backgroundColor = new Color(108, 108, 108, 255);
    private Color backgroundColorLight = new Color(75, 75, 75, 255);
    private long startTime = 0;
    private boolean timer = false;
    private Thread searchWaiter = null;
    private boolean isUsing = false;
    private boolean isRunAsAdminPressed = false;
    private Pattern semicolon = Pattern.compile(";");
    private Pattern resultSplit = Pattern.compile(":");
    private int priorityCount = 0;
    private boolean isSearching = false;


    private SearchBar() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // 获取屏幕大小
        int width = screenSize.width;
        int height = screenSize.height;
        int searchBarWidth = (int) (width * 0.4);
        int searchBarHeight = (int) (height * 0.5);
        int positionX = width / 2 - searchBarWidth / 2;
        int positionY = height / 2 - searchBarHeight / 2;

        //frame
        searchBar.setBounds(positionX, positionY, searchBarWidth, searchBarHeight);
        searchBar.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        searchBar.setUndecorated(true);
        AWTUtilities.setWindowOpaque(searchBar, false);
        searchBar.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        searchBar.setBackground(null);
        searchBar.setOpacity(0.9f);
        panel = searchBar.getContentPane();
        //TODO 构建时修改
        final boolean debug = false;
        if (!debug) {
            searchBar.setType(JFrame.Type.UTILITY);//隐藏任务栏图标
        }
        //labels
        Font font = new Font("Microsoft JhengHei", Font.BOLD, (int) ((height * 0.1) / 96 * 72) / 4);
        Color fontColor = new Color(73, 162, 255, 255);
        label1.setSize(searchBarWidth, (int) (searchBarHeight * 0.2));
        label1.setLocation(0, (int) (searchBarHeight * 0.2));
        label1.setFont(font);
        label1.setForeground(fontColor);
        label1.setOpaque(true);
        label1.setBackground(null);


        label2.setSize(searchBarWidth, (int) (searchBarHeight * 0.2));
        label2.setLocation(0, (int) (searchBarHeight * 0.4));
        label2.setFont(font);
        label2.setForeground(fontColor);
        label2.setOpaque(true);
        label2.setBackground(null);


        label3.setSize(searchBarWidth, (int) (searchBarHeight * 0.2));
        label3.setLocation(0, (int) (searchBarHeight * 0.6));
        label3.setFont(font);
        label3.setForeground(fontColor);
        label3.setOpaque(true);
        label3.setBackground(null);


        label4.setSize(searchBarWidth, (int) (searchBarHeight * 0.2));
        label4.setLocation(0, (int) (searchBarHeight * 0.8));
        label4.setFont(font);
        label4.setForeground(fontColor);
        label4.setOpaque(true);
        label4.setBackground(null);


        URL icon = TaskBar.class.getResource("/icons/taskbar_32x32.png");
        Image image = new ImageIcon(icon).getImage();
        searchBar.setIconImage(image);
        searchBar.setBackground(new Color(0, 0, 0, 0));


        //TextField
        textField = new JTextField(300);
        textField.setSize(searchBarWidth, (int) (searchBarHeight * 0.2));
        Font textFieldFont = new Font("Microsoft JhengHei", Font.BOLD, (int) ((height * 0.1) / 96 * 72 / 1.2));
        textField.setFont(textFieldFont);
        textField.setForeground(Color.WHITE);
        textField.setHorizontalAlignment(JTextField.LEFT);
        textField.setBorder(null);
        textField.setBackground(backgroundColor);
        textField.setLocation(0, 0);
        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                if (SettingsFrame.isLoseFocusClose) {
                    closedTodo();
                }
            }
        });

        //panel
        panel.setLayout(null);
        panel.setBackground(new Color(0, 0, 0, 0));
        panel.add(textField);
        panel.add(label1);
        panel.add(label2);
        panel.add(label3);
        panel.add(label4);


        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(4);

        //刷新屏幕线程
        fixedThreadPool.execute(() -> {
            while (!mainExit) {
                try {
                    panel.repaint();
                } catch (Exception ignored) {

                } finally {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {

                    }
                }
            }
        });

        fixedThreadPool.execute(() -> {
            //检测缓存大小 过大时进行清理
            while (!mainExit) {
                if (!search.isManualUpdate() && !isUsing) {
                    if (search.getRecycleBinSize() > 3000) {
                        System.out.println("已检测到回收站过大，自动清理");
                        search.setUsable(false);
                        search.mergeAndClearRecycleBin();
                        search.setUsable(true);
                    }
                    if (search.getLoadListSize() > 15000) {
                        System.out.println("加载缓存空间过大，自动清理");
                        search.setUsable(false);
                        search.mergeFileToList();
                        search.setUsable(true);
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {

                }
            }
        });

        fixedThreadPool.execute(() -> {
            //保存缓存线程
            int cacheCount = 0;
            while (!mainExit) {
                cacheCount++;
                if (cacheCount > 50) {
                    cacheCount = 0;
                    if (listResult.size() > 0) {
                        ArrayList<String> listCache = new ArrayList<>();
                        for (String result : listResult) {
                            listCache.add(result + ";");
                            if (listCache.size() > 200) {
                                break;
                            }
                        }
                        saveCache(listCache);
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {

                }
            }
        });

        fixedThreadPool.execute(() -> {
            //接收insertUpdate的信息并进行搜索
            //停顿时间0.5s，每一次输入会更新一次startTime，该线程记录endTime
            while (!mainExit) {
                long endTime = System.currentTimeMillis();
                if ((endTime - startTime > 500) && (timer)) {
                    timer = false; //开始搜索 计时停止
                    labelCount = 0;
                    clearLabel();
                    if (!textField.getText().equals("")) {
                        label1.setBackground(labelColor);
                    } else {
                        clearLabel();
                    }
                    listResult.clear();
                    String text = textField.getText();
                    if (search.isUsable()) {
                        text = PinYinConverter.getPinYin(text);
                        char firstWord = '\0';
                        try {
                            firstWord = text.charAt(0);
                        } catch (Exception ignored) {

                        }
                        if (firstWord == ':') {
                            if (text.equals(":update")) {
                                clearLabel();
                                MainClass.showMessage("提示", "正在更新文件索引");
                                clearTextFieldText();
                                closedTodo();
                                search.setManualUpdate(true);
                                timer = false;
                                continue;
                            }
                            if (text.equals(":version")) {
                                clearLabel();
                                clearTextFieldText();
                                closedTodo();
                                JOptionPane.showMessageDialog(null, "当前版本：" + MainClass.version);
                            }
                            if (text.equals(":help")) {
                                clearLabel();
                                clearTextFieldText();
                                closedTodo();
                                JOptionPane.showMessageDialog(null, "帮助：\n" +
                                        "1.默认Ctrl + Alt + J打开搜索框\n" +
                                        "2.Enter键运行程序\n" +
                                        "3.Ctrl + Enter键打开并选中文件所在文件夹\n" +
                                        "4.Shift + Enter键以管理员权限运行程序（前提是该程序拥有管理员权限）\n" +
                                        "5.在搜索框中输入  : update  强制重建本地索引\n" +
                                        "6.在搜索框中输入  : version  查看当前版本\n" +
                                        "7.在搜索框中输入  : clearbin  清空回收站\n" +
                                        "8.在搜索框中输入  : help  查看帮助\"\n" +
                                        "9.在设置中可以自定义命令，在搜索框中输入  : 自定义标识  运行自己的命令\n" +
                                        "10.在输入的文件名后输入  : full  可全字匹配\n" +
                                        "11.在输入的文件名后输入  : file  可只匹配文件\n" +
                                        "12.在输入的文件名后输入  : folder  可只匹配文件夹\n" +
                                        "13.在输入的文件名后输入  : filefull  可只匹配文件并全字匹配\n" +
                                        "14.在输入的文件名后输入  : folderfull  可只匹配文件夹并全字匹配");
                            }
                            if (text.equals(":clearbin")) {
                                clearLabel();
                                clearTextFieldText();
                                closedTodo();
                                int r = JOptionPane.showConfirmDialog(null, "你确定要清空回收站吗");
                                if (r == 0) {
                                    try {
                                        File[] roots = File.listRoots();
                                        for (File root : roots) {
                                            Runtime.getRuntime().exec("cmd /c rd /s /q " + root.getAbsolutePath() + "$Recycle.Bin");
                                        }
                                        JOptionPane.showMessageDialog(null, "清空回收站成功");
                                    } catch (IOException e) {
                                        JOptionPane.showMessageDialog(null, "清空回收站失败");
                                    }
                                }
                            }
                            for (String i : SettingsFrame.cmdSet) {
                                String[] cmdInfo = semicolon.split(i);
                                if (cmdInfo[0].equals(text)) {
                                    clearLabel();
                                    clearTextFieldText();
                                    closedTodo();
                                    Desktop desktop;
                                    if (Desktop.isDesktopSupported()) {
                                        desktop = Desktop.getDesktop();
                                        try {
                                            desktop.open(new File(cmdInfo[1]));
                                        } catch (IOException e) {
                                            JOptionPane.showMessageDialog(null, "执行失败");
                                        }
                                    }
                                }
                            }
                        } else {
                            searchPriorityFolder(text);
                            searchCache(text);

                            ArrayList<String> paths = new ArrayList<>();
                            String listPath;
                            int ascII = getAscIISum(getFileName(text));

                            if (0 < ascII && ascII < 100) {
                                for (int i = 0; i < 2500; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (100 < ascII && ascII < 200) {
                                for (int i = 100; i < 500; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (200 < ascII && ascII < 300) {
                                for (int i = 200; i < 600; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (300 < ascII && ascII < 400) {
                                for (int i = 300; i < 700; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (400 < ascII && ascII < 500) {
                                for (int i = 400; i < 800; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (500 < ascII && ascII < 600) {
                                for (int i = 500; i < 900; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (600 < ascII && ascII < 700) {
                                for (int i = 600; i < 1000; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (700 < ascII && ascII < 800) {
                                for (int i = 700; i < 1100; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (800 < ascII && ascII < 900) {
                                for (int i = 800; i < 1200; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (900 < ascII && ascII < 1000) {
                                for (int i = 900; i < 1300; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (1000 < ascII && ascII < 1100) {
                                for (int i = 1000; i < 1400; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (1100 < ascII && ascII < 1200) {
                                for (int i = 1100; i < 1500; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (1200 < ascII && ascII < 1300) {
                                for (int i = 1200; i < 1600; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (1300 < ascII && ascII < 1400) {
                                for (int i = 1300; i < 1700; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (1400 < ascII && ascII < 1500) {
                                for (int i = 1400; i < 1800; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (1500 < ascII && ascII < 1600) {
                                for (int i = 1500; i < 1900; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (1600 < ascII && ascII < 1700) {
                                for (int i = 1600; i < 2000; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (1700 < ascII && ascII < 1800) {
                                for (int i = 1700; i < 2100; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (1800 < ascII && ascII < 1900) {
                                for (int i = 1800; i < 2200; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (1900 < ascII && ascII < 2000) {
                                for (int i = 1900; i < 2300; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (2000 < ascII && ascII < 2100) {
                                for (int i = 2000; i < 2400; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (2100 < ascII && ascII < 2200) {
                                for (int i = 2100; i < 2500; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }

                                addResult(paths, text, System.currentTimeMillis());
                            } else if (2200 < ascII && ascII < 2300) {
                                for (int i = 2200; i < 2500; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }
                                paths.add(SettingsFrame.dataPath + "\\list2500-.txt");
                                addResult(paths, text, System.currentTimeMillis());
                            } else if (2300 < ascII && ascII < 2400) {
                                for (int i = 2300; i < 2500; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }
                                paths.add(SettingsFrame.dataPath + "\\list2500-.txt");
                                addResult(paths, text, System.currentTimeMillis());
                            } else if (2400 < ascII && ascII < 2500) {
                                for (int i = 2400; i < 2500; i += 100) {
                                    int name = i + 100;
                                    listPath = SettingsFrame.dataPath + "\\list" + i + "-" + name + ".txt";
                                    paths.add(listPath);
                                }
                                paths.add(SettingsFrame.dataPath + "\\list2500-.txt");
                                addResult(paths, text, System.currentTimeMillis());
                            } else {
                                paths.add(SettingsFrame.dataPath + "\\list2500-.txt");
                                addResult(paths, text, System.currentTimeMillis());
                            }
                        }
                    } else {
                        if (search.isManualUpdate()) {
                            if (searchWaiter == null || !searchWaiter.isAlive()) {
                                searchWaiter = new Thread(() -> {
                                    while (!mainExit) {
                                        if (Thread.currentThread().isInterrupted()) {
                                            break;
                                        }
                                        if (search.isUsable()) {
                                            startTime = System.currentTimeMillis() - 500;
                                            timer = true;
                                            break;
                                        }
                                        try {
                                            Thread.sleep(20);
                                        } catch (InterruptedException ignored) {

                                        }
                                    }
                                });
                                searchWaiter.start();
                            }
                        }
                        clearLabel();
                        if (!search.isUsable()) {
                            label1.setBackground(labelColor);
                            label1.setText("正在建立索引...");
                        }
                    }
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {

                }
            }
        });

        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                clearLabel();
                listResult.clear();
                labelCount = 0;
                priorityCount = 0;
                startTime = System.currentTimeMillis();
                timer = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clearLabel();
                listResult.clear();
                labelCount = 0;
                priorityCount = 0;
                String t = textField.getText();

                if (t.equals("")) {
                    clearLabel();
                    listResult.clear();
                    labelCount = 0;
                    startTime = System.currentTimeMillis();
                } else {
                    startTime = System.currentTimeMillis();
                    timer = true;
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {

            }
        });

        textField.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent arg0) {
                if (!listResult.isEmpty()) {
                    int key = arg0.getKeyCode();
                    if (38 == key) {
                        //上键被点击
                        if (!isSearching) {
                            if (!textField.getText().equals("")) {
                                labelCount--;
                                if (labelCount < 0) {
                                    labelCount = 0;
                                }

                                //System.out.println(labelCount);
                                if (labelCount >= listResult.size()) {
                                    labelCount = listResult.size() - 1;
                                }
                                if (labelCount < 3) {
                                    //未到最上端
                                    if (0 == labelCount) {
                                        label1.setBackground(labelColor);
                                        try {
                                            String text = label2.getText();
                                            if (text.equals("")) {
                                                label2.setBackground(null);
                                            } else {
                                                label2.setBackground(backgroundColor);
                                            }
                                        } catch (NullPointerException e) {
                                            label2.setBackground(null);
                                        }
                                        try {
                                            String text = label3.getText();
                                            if (text.equals("")) {
                                                label3.setBackground(null);
                                            } else {
                                                label3.setBackground(backgroundColorLight);
                                            }
                                        } catch (NullPointerException e) {
                                            label3.setBackground(null);
                                        }
                                        try {
                                            String text = label4.getText();
                                            if (text.equals("")) {
                                                label4.setBackground(null);
                                            } else {
                                                label4.setBackground(backgroundColor);
                                            }
                                        } catch (NullPointerException e) {
                                            label4.setBackground(null);
                                        }
                                    } else if (1 == labelCount) {
                                        label1.setBackground(backgroundColorLight);
                                        label2.setBackground(labelColor);
                                        try {
                                            String text = label3.getText();
                                            if (text.equals("")) {
                                                label3.setBackground(null);
                                            } else {
                                                label3.setBackground(backgroundColorLight);
                                            }
                                        } catch (NullPointerException e) {
                                            label3.setBackground(null);
                                        }
                                        try {
                                            String text = label4.getText();
                                            if (text.equals("")) {
                                                label4.setBackground(null);
                                            } else {
                                                label4.setBackground(backgroundColor);
                                            }
                                        } catch (NullPointerException e) {
                                            label4.setBackground(null);
                                        }
                                    } else if (2 == labelCount) {
                                        label1.setBackground(backgroundColorLight);
                                        label2.setBackground(backgroundColor);
                                        label3.setBackground(labelColor);
                                        try {
                                            String text = label4.getText();
                                            if (text.equals("")) {
                                                label4.setBackground(null);
                                            } else {
                                                label4.setBackground(backgroundColor);
                                            }
                                        } catch (NullPointerException e) {
                                            label4.setBackground(null);
                                        }
                                    }
                                } else {
                                    //到达最下端
                                    label1.setBackground(backgroundColorLight);
                                    label2.setBackground(backgroundColor);
                                    label3.setBackground(backgroundColorLight);
                                    label4.setBackground(labelColor);
                                    String path = listResult.get(labelCount - 3);
                                    String name = getFileName(listResult.get(labelCount - 3));
                                    ImageIcon icon;
                                    if (isDirectory(path) || isFile(path)) {
                                        icon = (ImageIcon) GetIcon.getBigIcon(path);
                                        icon = changeIcon(icon, label1.getHeight() - 60, label1.getHeight() - 60);
                                        label1.setIcon(icon);
                                        label1.setText("<html><body>" + name + "<br>" + ">>>" + getParentPath(path) + "</body></html>");
                                    } else {
                                        label1.setIcon(null);
                                        label1.setText("无效文件");
                                        search.addToRecycleBin(path);
                                    }
                                    path = listResult.get(labelCount - 2);
                                    name = getFileName(listResult.get(labelCount - 2));

                                    if (isDirectory(path) || isFile(path)) {
                                        icon = (ImageIcon) GetIcon.getBigIcon(path);
                                        icon = changeIcon(icon, label1.getHeight() - 60, label1.getHeight() - 60);
                                        label2.setText("<html><body>" + name + "<br>" + ">>>" + getParentPath(path) + "</body></html>");
                                        label2.setIcon(icon);
                                    } else {
                                        label2.setIcon(null);
                                        label2.setText("无效文件");
                                        search.addToRecycleBin(path);
                                    }
                                    path = listResult.get(labelCount - 1);
                                    name = getFileName(listResult.get(labelCount - 1));


                                    if (isDirectory(path) || isFile(path)) {
                                        icon = (ImageIcon) GetIcon.getBigIcon(path);
                                        icon = changeIcon(icon, label1.getHeight() - 60, label1.getHeight() - 60);
                                        label3.setText("<html><body>" + name + "<br>" + ">>>" + getParentPath(path) + "</body></html>");
                                        label3.setIcon(icon);
                                    } else {
                                        label3.setIcon(null);
                                        label3.setText("无效文件");
                                        search.addToRecycleBin(path);
                                    }
                                    path = listResult.get(labelCount);
                                    name = getFileName(listResult.get(labelCount));


                                    if (isDirectory(path) || isFile(path)) {
                                        icon = (ImageIcon) GetIcon.getBigIcon(path);
                                        icon = changeIcon(icon, label1.getHeight() - 60, label1.getHeight() - 60);
                                        label4.setText("<html><body>" + name + "<br>" + ">>>" + getParentPath(path) + "</body></html>");
                                        label4.setIcon(icon);
                                    } else {
                                        label4.setIcon(null);
                                        label4.setText("无效文件");
                                        search.addToRecycleBin(path);
                                    }
                                }
                            }
                        }
                    } else if (40 == key) {
                        //下键被点击
                        if (!isSearching) {
                            boolean isNextExist = false;
                            if (labelCount == 0) {
                                try {
                                    if (!label2.getText().equals("")) {
                                        isNextExist = true;
                                    }
                                } catch (NullPointerException ignored) {

                                }
                            } else if (labelCount == 1) {
                                try {
                                    if (!label3.getText().equals("")) {
                                        isNextExist = true;
                                    }
                                } catch (NullPointerException ignored) {

                                }
                            } else if (labelCount == 2) {
                                try {
                                    if (!label4.getText().equals("")) {
                                        isNextExist = true;
                                    }
                                } catch (NullPointerException ignored) {

                                }
                            } else {
                                isNextExist = true;
                            }
                            if (isNextExist) {
                                if (!textField.getText().equals("")) {
                                    labelCount++;
                                    if (labelCount < 0) {
                                        labelCount = 0;
                                    }

                                    //System.out.println(labelCount);
                                    if (labelCount >= listResult.size()) {
                                        labelCount = listResult.size() - 1;
                                    }
                                    if (labelCount < 3) {
                                        //未到最下端
                                        if (0 == labelCount) {
                                            label1.setBackground(labelColor);
                                            try {
                                                String text = label2.getText();
                                                if (text.equals("")) {
                                                    label2.setBackground(null);
                                                } else {
                                                    label2.setBackground(backgroundColor);
                                                }
                                            } catch (NullPointerException e) {
                                                label2.setBackground(null);
                                            }
                                            try {
                                                String text = label3.getText();
                                                if (text.equals("")) {
                                                    label3.setBackground(null);
                                                } else {
                                                    label3.setBackground(backgroundColorLight);
                                                }
                                            } catch (NullPointerException e) {
                                                label3.setBackground(null);
                                            }
                                            try {
                                                String text = label4.getText();
                                                if (text.equals("")) {
                                                    label4.setBackground(null);
                                                } else {
                                                    label4.setBackground(backgroundColor);
                                                }
                                            } catch (NullPointerException e) {
                                                label4.setBackground(null);
                                            }
                                        } else if (1 == labelCount) {
                                            label1.setBackground(backgroundColorLight);
                                            label2.setBackground(labelColor);
                                            try {
                                                String text = label3.getText();
                                                if (text.equals("")) {
                                                    label3.setBackground(null);
                                                } else {
                                                    label3.setBackground(backgroundColorLight);
                                                }
                                            } catch (NullPointerException e) {
                                                label3.setBackground(null);
                                            }
                                            try {
                                                String text = label4.getText();
                                                if (text.equals("")) {
                                                    label4.setBackground(null);
                                                } else {
                                                    label4.setBackground(backgroundColor);
                                                }
                                            } catch (NullPointerException e) {
                                                label4.setBackground(null);
                                            }
                                        } else if (2 == labelCount) {
                                            label1.setBackground(backgroundColorLight);
                                            label2.setBackground(backgroundColor);
                                            label3.setBackground(labelColor);
                                            try {
                                                String text = label4.getText();
                                                if (text.equals("")) {
                                                    label4.setBackground(null);
                                                } else {
                                                    label4.setBackground(backgroundColor);
                                                }
                                            } catch (NullPointerException e) {
                                                label4.setBackground(null);
                                            }
                                        }
                                    } else {
                                        //到最下端
                                        label1.setBackground(backgroundColorLight);
                                        label2.setBackground(backgroundColor);
                                        label3.setBackground(backgroundColorLight);
                                        label4.setBackground(labelColor);
                                        String path = listResult.get(labelCount - 3);
                                        String name = getFileName(listResult.get(labelCount - 3));
                                        ImageIcon icon;
                                        if (isDirectory(path) || isFile(path)) {
                                            icon = (ImageIcon) GetIcon.getBigIcon(path);
                                            icon = changeIcon(icon, label1.getHeight() - 60, label1.getHeight() - 60);
                                            label1.setIcon(icon);
                                            label1.setText("<html><body>" + name + "<br>" + ">>>" + getParentPath(path) + "</body></html>");
                                        } else {
                                            label1.setIcon(null);
                                            label1.setText("无效文件");
                                            search.addToRecycleBin(path);
                                        }
                                        path = listResult.get(labelCount - 2);
                                        name = getFileName(listResult.get(labelCount - 2));


                                        if (isDirectory(path) || isFile(path)) {
                                            icon = (ImageIcon) GetIcon.getBigIcon(path);
                                            icon = changeIcon(icon, label1.getHeight() - 60, label1.getHeight() - 60);
                                            label2.setText("<html><body>" + name + "<br>" + ">>>" + getParentPath(path) + "</body></html>");
                                            label2.setIcon(icon);
                                        } else {
                                            label2.setIcon(null);
                                            label2.setText("无效文件");
                                            search.addToRecycleBin(path);
                                        }
                                        path = listResult.get(labelCount - 1);
                                        name = getFileName(listResult.get(labelCount - 1));

                                        if (isDirectory(path) || isFile(path)) {
                                            icon = (ImageIcon) GetIcon.getBigIcon(path);
                                            icon = changeIcon(icon, label1.getHeight() - 60, label1.getHeight() - 60);
                                            label3.setText("<html><body>" + name + "<br>" + ">>>" + getParentPath(path) + "</body></html>");
                                            label3.setIcon(icon);
                                        } else {
                                            label3.setIcon(null);
                                            label3.setText("无效文件");
                                            search.addToRecycleBin(path);
                                        }
                                        path = listResult.get(labelCount);
                                        name = getFileName(listResult.get(labelCount));


                                        if (isDirectory(path) || isFile(path)) {
                                            icon = (ImageIcon) GetIcon.getBigIcon(path);
                                            icon = changeIcon(icon, label1.getHeight() - 60, label1.getHeight() - 60);
                                            label4.setText("<html><body>" + name + "<br>" + ">>>" + getParentPath(path) + "</body></html>");
                                            label4.setIcon(icon);
                                        } else {
                                            label4.setIcon(null);
                                            label4.setText("无效文件");
                                            search.addToRecycleBin(path);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (10 == key) {
                        //enter被点击
                        closedTodo();
                        if (isOpenLastFolderPressed) {
                            //打开上级文件夹
                            File open = new File(listResult.get(labelCount));
                            try {
                                Runtime.getRuntime().exec("explorer.exe /select, \"" + open.getAbsolutePath() + "\"");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (SettingsFrame.isDefaultAdmin || isRunAsAdminPressed) {
                            openWithAdmin(listResult.get(labelCount));
                        } else {
                            openWithoutAdmin(listResult.get(labelCount));
                        }
                        //saveCache(listResult.get(labelCount) + ';');
                    } else if (SettingsFrame.openLastFolderKeyCode == key) {
                        //打开上级文件夹热键被点击
                        isOpenLastFolderPressed = true;
                    } else if (SettingsFrame.runAsAdminKeyCode == key) {
                        //以管理员方式运行热键被点击
                        isRunAsAdminPressed = true;
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent arg0) {
                int key = arg0.getKeyCode();
                if (SettingsFrame.openLastFolderKeyCode == key) {
                    //复位按键状态
                    isOpenLastFolderPressed = false;
                } else if (SettingsFrame.runAsAdminKeyCode == key) {
                    isRunAsAdminPressed = false;
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {

            }
        });
    }

    public static SearchBar getInstance() {
        return searchBarInstance;
    }

    private void initLabelColor() {
        int size = listResult.size();
        if (size == 0) {
            //无结果可以显示
            return;
        }
        if (size == 1) {
            label1.setBackground(labelColor);
            label2.setBackground(null);
            label3.setBackground(null);
            label4.setBackground(null);
        } else if (size == 2) {
            label1.setBackground(labelColor);
            label2.setBackground(backgroundColor);
            label3.setBackground(null);
            label4.setBackground(null);
        } else if (size == 3) {
            label1.setBackground(labelColor);
            label2.setBackground(backgroundColor);
            label3.setBackground(backgroundColorLight);
            label4.setBackground(null);
        } else {
            label1.setBackground(labelColor);
            label2.setBackground(backgroundColor);
            label3.setBackground(backgroundColorLight);
            label4.setBackground(backgroundColor);
        }
    }

    public boolean isUsing() {
        //窗口是否正在显示
        return this.isUsing;
    }

    private boolean isExist(String path) {
        File f = new File(path);
        return f.exists();
    }

    private void addResult(ArrayList<String> paths, String text, long time) {
        //为label添加结果
        isSearching = true;
        String[] strings = new String[0];
        String searchText;
        int length;
        try {
            strings = resultSplit.split(text);
            searchText = strings[0];
            length = strings.length;
        } catch (ArrayIndexOutOfBoundsException e) {
            searchText = "";
            length = 0;
        }
        String each;
        if (!searchText.equals("")) {
            label1.setText("搜索中");
        }
        for (String path : paths) {
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                while ((each = br.readLine()) != null) {
                    if (startTime > time) { //用户重新输入了信息
                        isSearching = false;
                        return;
                    }
                    if (search.isUsable()) {
                        if (length != 2 && match(getFileName(each), searchText)) {
                            if (!listResult.contains(each)) {
                                if (isExist(each)) {
                                    if (canExecute(each)) {
                                        listResult.add(priorityCount, each);
                                    } else {
                                        listResult.add(each);
                                    }
                                    if (listResult.size() > 100) {
                                        break;
                                    }
                                }
                            }
                        } else if (match(getFileName(each), searchText) && length == 2) {
                            switch (strings[1].toUpperCase()) {
                                case "FILE":
                                    if (isFile(each)) {
                                        if (!listResult.contains(each)) {
                                            if (isExist(each)) {
                                                if (canExecute(each)) {
                                                    listResult.add(priorityCount, each);
                                                } else {
                                                    listResult.add(each);
                                                }
                                                if (listResult.size() > 100) {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case "FOLDER":
                                    if (isDirectory(each)) {
                                        if (!listResult.contains(each)) {
                                            if (isExist(each)) {
                                                if (canExecute(each)) {
                                                    listResult.add(priorityCount, each);
                                                } else {
                                                    listResult.add(each);
                                                }
                                                if (listResult.size() > 100) {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case "FULL":
                                    if (PinYinConverter.getPinYin(getFileName(each.toLowerCase())).equals(searchText.toLowerCase())) {
                                        if (!listResult.contains(each)) {
                                            if (isExist(each)) {
                                                if (canExecute(each)) {
                                                    listResult.add(priorityCount, each);
                                                } else {
                                                    listResult.add(each);
                                                }
                                                if (listResult.size() > 100) {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case "FOLDERFULL":
                                    if (PinYinConverter.getPinYin(getFileName(each.toLowerCase())).equals(searchText.toLowerCase())) {
                                        if (isDirectory(each)) {
                                            if (!listResult.contains(each)) {
                                                if (isExist(each)) {
                                                    if (canExecute(each)) {
                                                        listResult.add(priorityCount, each);
                                                    } else {
                                                        listResult.add(each);
                                                    }
                                                    if (listResult.size() > 100) {
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case "FILEFULL":
                                    if (PinYinConverter.getPinYin(getFileName(each.toLowerCase())).equals(searchText.toLowerCase())) {
                                        if (isFile(each)) {
                                            if (!listResult.contains(each)) {
                                                if (isExist(each)) {

                                                    if (canExecute(each)) {
                                                        listResult.add(priorityCount, each);
                                                    } else {
                                                        listResult.add(each);
                                                    }
                                                    if (listResult.size() > 100) {
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    break;
                            }
                        }
                    }
                }
            } catch (IOException ignored) {

            }
        }
        isSearching = false;
        if (!textField.getText().equals("")) {
            delRepeated();
            showResults();
            if (listResult.size() == 0) {
                label1.setText("无结果");
            }
        }
    }

    public void showSearchbar() {
        textField.setCaretPosition(0);
        //添加更新文件
        textField.requestFocusInWindow();
        isUsing = true;
        searchBar.setVisible(true);
    }

    private boolean canExecute(String path) {
        String name = getFileName(path);
        if (name.length() > 40) {
            return false;
        }
        String[] eachExd = semicolon.split(SettingsFrame.exds);
        for (String each : eachExd) {
            if (name.toLowerCase().endsWith("." + each.toLowerCase())) {
                return true;
            }
        }
        return new File(path).isDirectory();
    }

    private void showResults() {
        initLabelColor();
        try {
            String path = listResult.get(0);
            String name = getFileName(listResult.get(0));
            ImageIcon icon;
            if (isDirectory(path) || isFile(path)) {
                icon = (ImageIcon) GetIcon.getBigIcon(path);
                icon = changeIcon(icon, label1.getHeight() - 60, label1.getHeight() - 60);
                label1.setIcon(icon);
                label1.setText("<html><body>" + name + "<br>" + ">>>" + getParentPath(path) + "</body></html>");
            } else {
                label1.setIcon(null);
                label1.setText("无效文件");
                search.addToRecycleBin(path);
            }
            path = listResult.get(1);
            name = getFileName(listResult.get(1));


            if (isDirectory(path) || isFile(path)) {
                icon = (ImageIcon) GetIcon.getBigIcon(path);
                icon = changeIcon(icon, label1.getHeight() - 60, label1.getHeight() - 60);
                label2.setText("<html><body>" + name + "<br>" + ">>>" + getParentPath(path) + "</body></html>");
                label2.setIcon(icon);
            } else {
                label2.setIcon(null);
                label2.setText("无效文件");
                search.addToRecycleBin(path);
            }
            path = listResult.get(2);
            name = getFileName(listResult.get(2));


            if (isDirectory(path) || isFile(path)) {
                icon = (ImageIcon) GetIcon.getBigIcon(path);
                icon = changeIcon(icon, label1.getHeight() - 60, label1.getHeight() - 60);
                label3.setIcon(icon);
                label3.setText("<html><body>" + name + "<br>" + ">>>" + getParentPath(path) + "</body></html>");
            } else {
                label3.setIcon(null);
                label3.setText("无效文件");
                search.addToRecycleBin(path);
            }
            path = listResult.get(3);
            name = getFileName(listResult.get(3));


            if (isDirectory(path) || isFile(path)) {
                icon = (ImageIcon) GetIcon.getBigIcon(path);
                icon = changeIcon(icon, label1.getHeight() - 60, label1.getHeight() - 60);
                label4.setText("<html><body>" + name + "<br>" + ">>>" + getParentPath(path) + "</body></html>");
                label4.setIcon(icon);
            } else {
                label4.setIcon(null);
                label4.setText("无效文件");
                search.addToRecycleBin(path);
            }
        } catch (java.lang.IndexOutOfBoundsException ignored) {

        }
    }


    private void clearLabel() {
        label1.setIcon(null);
        label2.setIcon(null);
        label3.setIcon(null);
        label4.setIcon(null);
        label1.setText(null);
        label2.setText(null);
        label3.setText(null);
        label4.setText(null);
        label1.setBackground(null);
        label2.setBackground(null);
        label3.setBackground(null);
        label4.setBackground(null);
    }

    private void openWithAdmin(String path) {
        File name = new File(path);
        if (name.exists()) {
            try {
                try {
                    Runtime.getRuntime().exec("\"" + name.getAbsolutePath() + "\"", null, name.getParentFile());
                } catch (IOException e) {
                    Desktop desktop;
                    if (Desktop.isDesktopSupported()) {
                        desktop = Desktop.getDesktop();
                        desktop.open(name);
                    }
                }
            } catch (IOException e) {
                //打开上级文件夹
                try {
                    Runtime.getRuntime().exec("explorer.exe /select, \"" + name.getAbsolutePath() + "\"");
                } catch (IOException ignored) {

                }
            }
        }
    }

    private void openWithoutAdmin(String path) {
        File name = new File(path);
        if (name.exists()) {
            try {
                try {
                    if (path.endsWith("exe")) {
                        Runtime.getRuntime().exec("cmd /c runas /trustlevel:0x20000 \"" + name.getAbsolutePath() + "\"", null, name.getParentFile());
                    } else {
                        File fileToOpen = new File("fileToOpen.txt");
                        File fileOpener = new File("fileOpener.exe");
                        try (BufferedWriter buffw = new BufferedWriter(new FileWriter(fileToOpen))) {
                            buffw.write(name.getAbsolutePath() + "\n");
                            buffw.write(name.getParent());
                        }
                        Runtime.getRuntime().exec("cmd /c runas /trustlevel:0x20000 \"" + fileOpener.getAbsolutePath() + "\"");
                    }
                } catch (IOException e) {
                    Desktop desktop;
                    if (Desktop.isDesktopSupported()) {
                        desktop = Desktop.getDesktop();
                        desktop.open(name);
                    }
                }
            } catch (IOException e) {
                //打开上级文件夹
                try {
                    Runtime.getRuntime().exec("explorer.exe /select, \"" + name.getAbsolutePath() + "\"");
                } catch (IOException ignored) {

                }
            }
        }
    }

    private String getFileName(String path) {
        File file = new File(path);
        return file.getName();
    }

    private ImageIcon changeIcon(ImageIcon icon, int width, int height) {
        try {
            Image image = icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT);
            return new ImageIcon(image);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public int getAscIISum(String path) {
        path = path.toUpperCase();
        int sum = 0;
        for (char i : path.toCharArray()) {
            sum += i;
        }
        return sum;
    }

    private void saveCache(ArrayList<String> list) {
        int cacheNum = 0;
        File cache = new File("cache.dat");
        StringBuilder oldCaches = new StringBuilder();
        if (cache.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(cache))) {
                String eachLine;
                while ((eachLine = reader.readLine()) != null) {
                    oldCaches.append(eachLine);
                    cacheNum++;
                }
            } catch (IOException ignored) {

            }
        }
        StringBuilder strb = new StringBuilder();
        for (String each : list) {
            if (!oldCaches.toString().contains(each)) {
                cacheNum++;
                strb.append(each).append("\r\n");
            }
        }
        if (cacheNum < SettingsFrame.cacheNumLimit) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter("cache.dat", true))) {
                bw.write(strb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //超过限制，进行覆写
            try (BufferedWriter bw = new BufferedWriter(new FileWriter("cache.dat", false))) {
                bw.write(strb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void delCacheRepeated() {
        File cacheFile = new File("cache.dat");
        HashSet<String> set = new HashSet<>();
        StringBuilder allCaches = new StringBuilder();
        String eachLine;
        if (cacheFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(cacheFile))) {
                while ((eachLine = br.readLine()) != null) {
                    String[] each = semicolon.split(eachLine);
                    Collections.addAll(set, each);
                }
            } catch (IOException ignored) {

            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(cacheFile))) {
                for (String cache : set) {
                    allCaches.append(cache).append(";\n");
                }
                bw.write(allCaches.toString());
            } catch (IOException ignored) {

            }
        }
    }

    private void delCache(ArrayList<String> cache) {
        File cacheFile = new File("cache.dat");
        StringBuilder allCaches = new StringBuilder();
        String eachLine;
        if (cacheFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(cacheFile))) {
                while ((eachLine = br.readLine()) != null) {
                    String[] each = semicolon.split(eachLine);
                    for (String eachCache : each) {
                        if (!(cache.contains(eachCache))) {
                            allCaches.append(eachCache).append(";\n");
                        }
                    }
                }
            } catch (IOException ignored) {

            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(cacheFile))) {
                bw.write(allCaches.toString());
            } catch (IOException ignored) {

            }
        }
    }

    private void searchCache(String searchFile) {
        String cacheResult;
        boolean isCacheRepeated = false;
        ArrayList<String> cachesToDel = new ArrayList<>();
        File cache = new File("cache.dat");
        if (cache.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(cache))) {
                while ((cacheResult = reader.readLine()) != null) {
                    String[] caches = semicolon.split(cacheResult);
                    for (String cach : caches) {
                        if (!(new File(cach).exists())) {
                            cachesToDel.add(cach);
                        } else {
                            String eachCacheName = getFileName(cach);
                            if (match(eachCacheName, searchFile)) {
                                if (!listResult.contains(cach)) {
                                    if (canExecute(cach)) {
                                        listResult.add(priorityCount, cach);
                                    } else {
                                        listResult.add(cach);
                                    }
                                    if (listResult.size() > 500) {
                                        break;
                                    }
                                } else {
                                    isCacheRepeated = true;
                                }
                            }
                        }
                    }
                }
            } catch (IOException ignored) {

            }
        }
        delCache(cachesToDel);
        if (isCacheRepeated) {
            delCacheRepeated();
        }
    }

    private boolean match(String srcText, String txt) {
        if (!txt.equals("")) {
            srcText = PinYinConverter.getPinYin(srcText).toLowerCase();
            txt = PinYinConverter.getPinYin(txt).toLowerCase();
            if (srcText.length() >= txt.length()) {
                return srcText.contains(txt);
            }
        }
        return false;
    }

    private void delRepeated() {
        LinkedHashSet<String> set = new LinkedHashSet<>(listResult);
        listResult.clear();
        listResult.addAll(set);
    }

    private void searchPriorityFolder(String text) {
        File path = new File(SettingsFrame.priorityFolder);
        boolean exist = path.exists();
        LinkedList<File> listRemain = new LinkedList<>();
        if (exist) {
            File[] files = path.listFiles();
            if (!(null == files || files.length == 0)) {
                for (File each : files) {
                    if (match(getFileName(each.getAbsolutePath()), text)) {
                        priorityCount++;
                        listResult.add(0, each.getAbsolutePath());
                    }
                    if (each.isDirectory()) {
                        listRemain.add(each);
                    }
                }
                while (!listRemain.isEmpty()) {
                    File remain = listRemain.pop();
                    File[] allFiles = remain.listFiles();
                    assert allFiles != null;
                    for (File each : allFiles) {
                        if (match(getFileName(each.getAbsolutePath()), text)) {
                            priorityCount++;
                            listResult.add(0, each.getAbsolutePath());
                        }
                        if (each.isDirectory()) {
                            listRemain.add(each);
                        }
                    }
                }
            }
        }
    }

    private void clearTextFieldText() {
        Runnable clear = () -> textField.setText(null);
        SwingUtilities.invokeLater(clear);
    }

    public void closedTodo() {
        Runnable todo = () -> {
            if (searchBar.isVisible()) {
                searchBar.setVisible(false);
            }
            CheckHotKey hotkeyListener = CheckHotKey.getInstance();
            hotkeyListener.setShowSearchBar(false);
            clearLabel();
            startTime = System.currentTimeMillis();//结束搜索
            isUsing = false;
            labelCount = 0;
            priorityCount = 0;
            listResult.clear();
            textField.setText(null);
            try {
                searchWaiter.interrupt();
            } catch (NullPointerException ignored) {

            }
        };
        SwingUtilities.invokeLater(todo);
    }

    private String getParentPath(String path) {
        File f = new File(path);
        return f.getParent();
    }

    private boolean isFile(String text) {
        File file = new File(text);
        return file.isFile();
    }

    private boolean isDirectory(String text) {
        File file = new File(text);
        return file.isDirectory();
    }
}

