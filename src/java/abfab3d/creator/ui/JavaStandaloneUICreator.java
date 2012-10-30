/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2011
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package abfab3d.creator.ui;

// External Imports
import java.util.*;
import java.io.*;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

// Internal Imports
import abfab3d.creator.*;

/**
 * Create a user interface for an editor using a standalone Java Application.
 *
 * @author Alan Hudson
 */
public class JavaStandaloneUICreator {
    /** The type of editors */
    private enum Editors {TEXTFIELD, COMBOBOX, FILE_DIALOG};

    private HashMap<Integer,String> indentCache;

    private GeometryKernel kernel;

    /** The number of steps */
    private List<Step> steps;

    public JavaStandaloneUICreator() {
        indentCache = new HashMap<Integer,String>();
    }

    /**
     * Create a user interface for a kernel.
     *
     * @param dir The directory to place the files
     * @param genParams Parameters for generation
     * @param kernel The kernel
     * @param remove The parameters to remove
     */
    public void createInterface(String packageName, String className, String title, String dir, List<Step> steps, Map<String,String> genParams, GeometryKernel kernel, Set<String> remove) {
        this.kernel = kernel;
        this.steps = new ArrayList<Step>();
        this.steps.addAll(steps);

        try {
            File f = new File(dir);

            if (!f.exists()) {
                f.mkdirs();
            }

            FileOutputStream fos = new FileOutputStream(f.toString() + "/" + className + ".java");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            PrintStream ps = new PrintStream(bos);

            Map<String,Parameter> params = kernel.getParams();

            ps.println("package " + packageName + ";");  // TODO: Need to add package name
            ps.println("");
            ps.println("import java.util.*;");
            ps.println("import javax.swing.*;");
            ps.println("import java.awt.GridLayout;");
            ps.println("import java.awt.Font;");
            ps.println("import java.awt.event.*;");
            ps.println("import java.io.*;");
            ps.println("import java.util.prefs.*;");
            ps.println("import abfab3d.creator.GeometryKernel;");
            ps.println("import abfab3d.creator.KernelResults;");
            ps.println("import abfab3d.creator.util.ParameterUtil;");
            ps.println("import org.web3d.vrml.export.PlainTextErrorReporter;");
            ps.println("import org.web3d.vrml.sav.BinaryContentHandler;");
            ps.println("import org.web3d.vrml.export.*;");
            ps.println("import app.common.*;");
            ps.println("import app.common.upload.shapeways.oauth.*;");
            
            ps.println("");
            ps.println("public class " + className + " extends JFrame implements ActionListener {");

            addGlobalVars(ps, params, remove);

            ps.println("    public " + className + "(String name) { super(name); }");
            ps.println("    public static void main(String[] args) {");
            ps.println("        " + className + " editor = new " + className + "(\"" + title + "\");");
            ps.println("        editor.launch();");
            ps.println("    }");
            ps.println();
            ps.println("    public void launch() {");
        	ps.println("        prefs = Preferences.userNodeForPackage(this.getClass());");
//            ps.println("        params = kernel.getParams();");
            ps.println("        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);");
            ps.println("        setupUI();");
            ps.println("        pack();");
            ps.println("        setVisible(true);");
            ps.println("    }");
            ps.println("    public void setupUI() {");


            ArrayList sorted_params = new ArrayList();
            sorted_params.addAll(params.values());

            java.util.Collections.sort(sorted_params);

            Iterator<Parameter> itr = sorted_params.iterator();

            // Create the menu bar
            createMenu(ps);
            
            ps.println(indent(8) + "GridLayout layout = new GridLayout(" + (params.size() - remove.size() + 1 + steps.size() + (steps.size() - 1)) + ",3);");
            ps.println(indent(8) + "setLayout(layout);");
            ps.println();

            itr = sorted_params.iterator();
            int curr_step = -1;

            while(itr.hasNext()) {
                Parameter p = itr.next();
                if (remove.contains(p.getName())) {
                    continue;
                }

                if (curr_step != p.getStep()) {
                    // Add step line
                    curr_step = p.getStep();
                    Step step = steps.get(curr_step);

                    if (curr_step != 0) {
                        // Add spacer
                        ps.println(indent(8) + "getContentPane().add(new JLabel(\"\"));");
                        ps.println(indent(8) + "getContentPane().add(new JLabel(\"\"));");
                        ps.println(indent(8) + "getContentPane().add(new JLabel(\"\"));");
                    }

                    ps.println(indent(8) + "JLabel step" + (curr_step+1) + " = new JLabel(\"Step: " + (curr_step+1) + "\");");
                    ps.println(indent(8) + "getContentPane().add(step" + (curr_step+1) + ");");
                    ps.println(indent(8) + "getContentPane().add(new JLabel(\"" + step.getDesc() + "\"));");
                    ps.println(indent(8) + "getContentPane().add(new JLabel(\"\"));");
                }

                addParameterUI(ps, p);
            }

            addGlobalButtons(ps);

            ps.println("    }");

            ps.println();
            addActions(ps,params,remove);

            ps.println("}");

            ps.flush();
            fos.close();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        System.out.println("Done creating editor");
    }

    public void createMenu(PrintStream ps) {
    	ps.println(indent(8) + "JMenuBar menubar = new JMenuBar();");
    	ps.println(indent(8) + "JMenu filemenu = new JMenu(\"File\");");
    	ps.println(indent(8) + "fileOpen = new JMenuItem(\"Open\");");
    	ps.println(indent(8) + "fileSave = new JMenuItem(\"Save\");");
    	ps.println(indent(8) + "fileOpen.addActionListener(this);");
    	ps.println(indent(8) + "fileSave.addActionListener(this);");
    	ps.println(indent(8) + "filemenu.add(fileOpen);");
    	ps.println(indent(8) + "filemenu.add(fileSave);");
    	ps.println(indent(8) + "menubar.add(filemenu);");
    	ps.println(indent(8) + "setJMenuBar(menubar);");
    	ps.println(indent(8) + "openDialog = new JFileChooser(new File(prefs.get(LAST_DIR, DEFAULT_DIR)));");
    	ps.println(indent(8) + "saveDialog = new JFileChooser(new File(prefs.get(LAST_DIR, DEFAULT_DIR)));");
    }
    
    /**
     * Add a user interface element for an item.
     *
     * @param ps The stream to print too
     * @param p The parameter
     */
    private void addParameterUI(PrintStream ps, Parameter p) {
System.out.println("Adding param: " + p.getName());
        ps.println(indent(8) + "JLabel " + p.getName() + "Label = new JLabel(\"" + p.getNameDesc() + "\");");
        ps.println(indent(8) + p.getName() + "Label.setToolTipText(\"" + p.getDesc() + "\");");
        ps.println(indent(8) + "Font " + (p.getName()) + "Font = " + p.getName() + "Label.getFont();");
        ps.println(indent(8) + p.getName()  + "Label.setFont(" + p.getName() + "Font.deriveFont(" + p.getName() + "Font.getStyle() ^ Font.BOLD));");

        ps.println(indent(8) + "getContentPane().add(" + p.getName() + "Label);");

        switch(getEditor(p)) {
            case COMBOBOX:
                ps.println(indent(8) + "String[] " + p.getName() + "Enums = new String[] {");
                String[] vals = null;

                if (p.getDataType() == Parameter.DataType.BOOLEAN) {
                    vals = new String[] {"true", "false"};
                } else {
                    vals = p.getEnumValues();
                }

                ps.print(indent(12));
                for(int i=0; i < vals.length; i++) {
                    ps.print("\"" + vals[i] + "\"");
                    if (i < vals.length - 1) {
                        ps.print(",");
                    }
                }
                ps.println("};");

                ps.println(indent(8) + p.getName() + "Editor = new JComboBox(" + p.getName() + "Enums);");
                int idx = 0;

                String[] evals = p.getEnumValues();
                if (evals != null) {
                    for(int i=0; i < evals.length; i++) {
                        if (evals[i].equals(p.getDefaultValue())) {
                            idx = i;
                        }
                    }
                    ps.println("((JComboBox)" + p.getName() + "Editor).setSelectedIndex(" + idx + ");");
                } else {
                    if (p.getDataType() == Parameter.DataType.BOOLEAN) {
                        vals = new String[] {"true", "false"};
                        if (p.getDefaultValue().equalsIgnoreCase("true")) {
                            ps.println("((JComboBox)" + p.getName() + "Editor).setSelectedIndex(" + 0 + ");");
                        } else {
                            ps.println("((JComboBox)" + p.getName() + "Editor).setSelectedIndex(" + 1 + ");");
                        }
                    } else {
                        vals = p.getEnumValues();
                    }
                }
                break;
            case FILE_DIALOG:
                if (p.getDefaultValue() == null) {
                    ps.println(indent(8) + "String dir_" + p.getName() + " = \".\";");
                } else {
                    ps.println(indent(8) + "String dir_" + p.getName() + " = \"" + p.getDefaultValue() + "\";");
                }
                ps.println(indent(8) + p.getName() + "Dialog = new JFileChooser(new File(dir_" + p.getName() + "));");
                ps.println(indent(8) + p.getName() + "Button = new JButton(\"Browse\");");
                ps.println(indent(8) + p.getName() + "Button.addActionListener(this);");
            default:
                ps.println(indent(8) + p.getName() + "Editor = new JTextField(\"" + p.getDefaultValue() + "\");");
        }
        ps.println(indent(8) + "getContentPane().add(" + p.getName() + "Editor);");

        // Determine third column content
        switch(getEditor(p)) {
            case FILE_DIALOG:
                ps.println(indent(8) + "getContentPane().add(" + p.getName() + "Button);");
                break;
            default:
                ps.println(indent(8) + "getContentPane().add(new JLabel(\"\"));");
        }

        ps.println();

    }

    /**
     * Add user interface elements for Global Buttons
     *
     * @param ps The stream to print too
     */
    private void addGlobalButtons(PrintStream ps) {
        ps.println(indent(8) + "submitButton = new JButton(\"Generate\");");
        ps.println(indent(8) + "getContentPane().add(submitButton);");
        ps.println(indent(8) + "submitButton.addActionListener(this);");

        ps.println(indent(8) + "printButton = new JButton(\"Check Printability\");");
        ps.println(indent(8) + "getContentPane().add(printButton);");
        ps.println(indent(8) + "printButton.addActionListener(this);");

        ps.println(indent(8) + "uploadButton = new JButton(\"Upload\");");
        ps.println(indent(8) + "getContentPane().add(uploadButton);");
        ps.println(indent(8) + "uploadButton.addActionListener(this);");
    }

    private void addActions(PrintStream ps, Map<String,Parameter> params, Set<String> remove) {

        // submit button
        ps.println(indent(4) + "public void actionPerformed(ActionEvent e) {");
        ps.println(indent(8) + "if (e.getSource() == submitButton) {");
        ps.println(indent(12) + "// Get all params to global string vars");

        setParams(ps, params, remove);

        // Generate Geometry
        ps.println(indent(12) + "try {");
        ps.println(indent(16) + "String filename = \"/tmp/out.x3d\";");
        ps.println(indent(16) + "FileOutputStream fos = new FileOutputStream(filename);");
        ps.println(indent(16) + "BufferedOutputStream bos = new BufferedOutputStream(fos);");
        ps.println(indent(16) + "PlainTextErrorReporter console = new PlainTextErrorReporter();");
//        ps.println(indent(16) + "BinaryContentHandler writer = (BinaryContentHandler) new X3DBinaryRetainedDirectExporter(bos, 3, 0, console, X3DBinarySerializer.METHOD_FASTEST_PARSING, 0.001f, true);");
        ps.println(indent(16) + "BinaryContentHandler writer = (BinaryContentHandler) new X3DXMLRetainedExporter(fos, 3, 0, console, 6);");
        ps.println(indent(16) + "KernelResults results = kernel.generate(parsed_params, GeometryKernel.Accuracy.VISUAL, writer);");
        ps.println(indent(16) + "fos.close();");

        ps.println(indent(16) + "double[] bounds_min = results.getMinBounds();");
        ps.println(indent(16) + "double[] bounds_max = results.getMaxBounds();");

        ps.println(indent(16) + "double max_axis = Math.max(bounds_max[0] - bounds_min[0], bounds_max[1] - bounds_min[1]);");
        ps.println(indent(16) + "max_axis = Math.max(max_axis, bounds_max[2] - bounds_min[2]);");

        ps.println(indent(16) + "double z = 2 * max_axis / Math.tan(Math.PI / 4);");
        ps.println(indent(16) + "float[] pos = new float[] {0,0,(float) z};");

        ps.println(indent(16) + "X3DViewer.viewX3DOM(\"out.x3d\",pos);");

        ps.println(indent(12) + "} catch(IOException ioe) { ioe.printStackTrace(); }");
        ps.println(indent(12) + "System.out.println(\"Model Done\");");

        ps.println(indent(8) + "} else if (e.getSource() == printButton) {");
        ps.println(indent(12) + "// Get all params to global string vars");

        setParams(ps, params, remove);

        // Printability Check
        // TODO: Outputting as .x3d for visualization, might not want to do that in general
        String tmp_dir = "/tmp/";
        ps.println(indent(12) + "try {");
        ps.println(indent(16) + "String filename = \"" + tmp_dir + "out.x3d\";");
        ps.println(indent(16) + "FileOutputStream fos = new FileOutputStream(filename);");
        ps.println(indent(16) + "BufferedOutputStream bos = new BufferedOutputStream(fos);");
        ps.println(indent(16) + "PlainTextErrorReporter console = new PlainTextErrorReporter();");
//        ps.println(indent(16) + "BinaryContentHandler writer = (BinaryContentHandler) new X3DBinaryRetainedDirectExporter(bos, 3, 0, console, X3DBinarySerializer.METHOD_FASTEST_PARSING, 0.001f, true);");
        ps.println(indent(16) + "BinaryContentHandler writer = (BinaryContentHandler) new X3DXMLRetainedExporter(bos, 3, 0, console, 6);");
        ps.println(indent(16) + "KernelResults results = kernel.generate(parsed_params, GeometryKernel.Accuracy.PRINT, writer);");
        ps.println(indent(16) + "bos.flush();");
        ps.println(indent(16) + "bos.close();");
        ps.println(indent(16) + "fos.close();");


        ps.println(indent(16) + "WallThicknessRunner wtr = new WallThicknessRunner();");
        ps.println(indent(16) + "String material = (String) parsed_params.get(\"material\");");
        ps.println(indent(16) + "WallThicknessResult res = wtr.runWallThickness(\"" + tmp_dir + "out.x3d\", material);");
        ps.println(indent(16) + "double[] bounds_min = results.getMinBounds();");
        ps.println(indent(16) + "double[] bounds_max = results.getMaxBounds();");

        ps.println(indent(16) + "double max_axis = Math.max(bounds_max[0] - bounds_min[0], bounds_max[1] - bounds_min[1]);");
        ps.println(indent(16) + "max_axis = Math.max(max_axis, bounds_max[2] - bounds_min[2]);");

        ps.println(indent(16) + "double z = 2 * max_axis / Math.tan(Math.PI / 4);");
        ps.println(indent(16) + "float[] pos = new float[] {0,0,(float) z};");

        // Make the fully qualified path be relative
        ps.println(indent(16) + "String viz = res.getVisualization();");
        ps.println(indent(16) + "if (viz != null) viz = viz.replace(\"" + tmp_dir + "\",\"\");");
        ps.println(indent(16) + "X3DViewer.viewX3DOM(new String[] {\"out.x3d\",viz},pos);");

        ps.println(indent(12) + "} catch(IOException ioe) { ioe.printStackTrace(); }");
        ps.println(indent(12) + "System.out.println(\"Printability Done\");");

        // Upload model
        ps.println(indent(8) + "} else if (e.getSource() == uploadButton) {");
        
        setParams(ps, params, remove);
        
        ps.println(indent(12) + "try {");
        ps.println(indent(16) + "System.out.println(\"Generating Model\");");
        ps.println(indent(16) + "String filename = \"/tmp/out.x3db\";");
        ps.println(indent(16) + "FileOutputStream fos = new FileOutputStream(filename);");
        ps.println(indent(16) + "BufferedOutputStream bos = new BufferedOutputStream(fos);");
        ps.println(indent(16) + "PlainTextErrorReporter console = new PlainTextErrorReporter();");
        ps.println(indent(16) + "BinaryContentHandler writer = (BinaryContentHandler) new X3DBinaryRetainedDirectExporter(bos, 3, 0, console, X3DBinarySerializer.METHOD_FASTEST_PARSING, 0.001f, true);");
        ps.println(indent(16) + "KernelResults results = kernel.generate(parsed_params, GeometryKernel.Accuracy.PRINT, writer);");
        ps.println(indent(16) + "fos.close();");

        ps.println(indent(16) + "System.out.println(\"Uploading Model\");");
        ps.println(indent(16) + "Integer modelId = null;");
        ps.println(indent(16) + "Float scale = 1.0f;");
        ps.println(indent(16) + "String title = \"Image Popper Model\";");
        ps.println(indent(16) + "String description = \"Generated by the ImagePopper creator\";");
        ps.println(indent(16) + "Integer isPublic = null;");  // default 1 - 3D model is public
        ps.println(indent(16) + "Integer viewState = null;"); // default 1 - 3D model is for sale
        ps.println(indent(16) + "ModelUploadOauthRunner uploader = new ModelUploadOauthRunner();");
        ps.println(indent(16) + "uploader.uploadModel(filename, modelId, scale, title, description, isPublic, viewState);");
        ps.println(indent(12) + "} catch(IOException ioe) { ioe.printStackTrace(); }");

        // Open a file
        ps.println(indent(8) + "} else if (e.getSource() == fileOpen) {");

        ps.println(indent(12) + "String lastDir = prefs.get(LAST_DIR, DEFAULT_DIR);");
        ps.println(indent(12) + "openDialog.setCurrentDirectory(new File(lastDir));");
        ps.println(indent(12) + "int returnVal = openDialog.showOpenDialog(this);");
        ps.println(indent(12) + "try {");
        ps.println(indent(16) + "if (returnVal == JFileChooser.APPROVE_OPTION) {");
        ps.println(indent(20) + "File selectedFile = openDialog.getSelectedFile();");
        ps.println(indent(20) + "if (selectedFile.exists()) {");
        ps.println(indent(24) + "FileInputStream fis = new FileInputStream(selectedFile);");
        ps.println(indent(24) + "prefs.put(LAST_DIR, selectedFile.getParent());");
        ps.println(indent(24) + "Properties props = new Properties();");
        ps.println(indent(24) + "props.load(fis);");
        ps.println(indent(24) + "Enumeration en = props.propertyNames();");
        ps.println(indent(24) + "while(en.hasMoreElements()) {");
        ps.println(indent(28) + "String key = (String) en.nextElement();");
        ps.println(indent(28) + "String val = (String) props.getProperty(key);");
//        ps.println(indent(28) + "System.out.println(key + \" = \" + val);");
        
    	Iterator<Parameter> itr = params.values().iterator();
    	int count = 0;

        while(itr.hasNext()) {
            Parameter p = itr.next();
            String name = p.getName();
            if (remove.contains(name)) {
                continue;
            }
            
            if (count == 0) {
            	ps.println(indent(28) + "if (key.equals(\"" + name + "\")) {");
            } else {
            	ps.println(indent(28) + "else if (key.equals(\"" + name + "\")) {");
            }
            
            switch(getEditor(p)) {
                case TEXTFIELD:
                    ps.println(indent(32) + "((JTextField)" + name + "Editor).setText(val);");
                    break;
                case FILE_DIALOG:
                    ps.println(indent(32) + "((JTextField)" + name + "Editor).setText(val);");
                    break;
                case COMBOBOX:
                	ps.println(indent(32) + "int count = ((JComboBox)" + name + "Editor).getItemCount();");
                	ps.println(indent(32) + "for (int i=0; i<count; i++) {");
                	ps.println(indent(36) + "String item = (String) ((JComboBox)" + name + "Editor).getItemAt(i);");
                	ps.println(indent(36) + "if (item.equals(val)) {");
                	ps.println(indent(40) + "((JComboBox)" + name + "Editor).setSelectedIndex(i);");
                	ps.println(indent(40) + "break;");
                	ps.println(indent(36) + "}");
                	ps.println(indent(32) + "}");
                default:
                    System.out.println("Unhandled action for editor: " + getEditor(p));
            }
            ps.println(indent(28) + "}");
            count++;
        }

        ps.println(indent(24) + "}");
        ps.println(indent(20) + "}");
        ps.println(indent(16) + "}");
        ps.println(indent(12) + "} catch(IOException ioe) { ioe.printStackTrace(); }");
        
        // Save a file
        ps.println(indent(8) + "} else if (e.getSource() == fileSave) {");

        ps.println(indent(12) + "String lastDir = prefs.get(LAST_DIR, DEFAULT_DIR);");
        ps.println(indent(12) + "saveDialog.setCurrentDirectory(new File(lastDir));");
        ps.println(indent(12) + "int returnVal = saveDialog.showSaveDialog(this);");
        ps.println(indent(12) + "if (returnVal == JFileChooser.APPROVE_OPTION) {");
        
        gatherParams(ps, params, remove);
//        ps.println(indent(16) + "System.out.println(properties);");
        ps.println(indent(16) + "File selectedFile = saveDialog.getSelectedFile();");
        ps.println(indent(16) + "prefs.put(LAST_DIR, selectedFile.getParent());");
        ps.println(indent(16) + "BufferedWriter bw = null;");
        ps.println(indent(16) + "try {");
        ps.println(indent(20) + "FileWriter fw = new FileWriter(selectedFile.getAbsoluteFile());");
        ps.println(indent(20) + "bw = new BufferedWriter(fw);");
        ps.println(indent(20) + "bw.write(properties);");
        ps.println(indent(16) + "} catch(IOException ioe) {");
        ps.println(indent(20) + "ioe.printStackTrace();");
        ps.println(indent(16) + "} finally {");
        ps.println(indent(20) + "try {");
        ps.println(indent(24) + "if (bw != null) bw.close();");
        ps.println(indent(20) + "} catch (IOException ioe) { }");
        ps.println(indent(16) + "}");
        ps.println(indent(12) + "}");
        
        itr = params.values().iterator();

        while(itr.hasNext()) {
            Parameter p = itr.next();
            if (remove.contains(p.getName())) {
                continue;
            }

            if (getEditor(p) == Editors.FILE_DIALOG) {
                ps.println(indent(8) + "} else if (e.getSource() == " + p.getName() + "Button) {");

                ps.println(indent(12) + "String lastDir = prefs.get(\"LAST_" + p.getName().toUpperCase() + "_DIR\", DEFAULT_DIR);");
                ps.println(indent(12) + p.getName() + "Dialog.setCurrentDirectory(new File(lastDir));");
                ps.println(indent(12) + "int returnVal = " + p.getName() + "Dialog" + ".showOpenDialog(this);");

                ps.println(indent(12) + "if (returnVal == JFileChooser.APPROVE_OPTION) {");
                ps.println(indent(16) + "File file = " + p.getName() + "Dialog" + ".getSelectedFile();");
                ps.println(indent(16) + "if (file.exists()) {");
                ps.println(indent(20) + "prefs.put(\"LAST_" + p.getName().toUpperCase() + "_DIR\", file.getParent());");
                ps.println(indent(20) + "((JTextField)" + p.getName() + "Editor).setText(file.toString());");
                ps.println(indent(16) + "}");
                ps.println(indent(12) + "}");
            }
        }

        ps.println(indent(8) + "}");

        ps.println(indent(4) + "}");
    }

    /**
     * Add global variables.
     */
    private void addGlobalVars(PrintStream ps, Map<String,Parameter> params, Set<String> remove) {
        Iterator<Parameter> itr = params.values().iterator();
        
//        ps.println(indent(4) + "Map<String,Parameter> params;");
        ps.println(indent(4) + "private static final String LAST_DIR = \"LAST_DIR\";");
        ps.println(indent(4) + "private static final String DEFAULT_DIR = \"/tmp\";");
        ps.println(indent(4) + "private Preferences prefs;");

        ps.println(indent(4) + "JButton submitButton;");
        ps.println(indent(4) + "JButton printButton;");
        ps.println(indent(4) + "JButton uploadButton;");
        ps.println(indent(4) + "JMenuItem fileOpen;");
        ps.println(indent(4) + "JMenuItem fileSave;");
        ps.println(indent(4) + "JFileChooser openDialog;");
        ps.println(indent(4) + "JFileChooser saveDialog;");
        
        while(itr.hasNext()) {
            Parameter p = itr.next();
            if (remove.contains(p.getName())) {
                continue;
            }

            ps.println(indent(4) + "/** " + p.getDesc() + " Field */");
            ps.println(indent(4) + "protected String " + p.getName() + ";");
            ps.println(indent(4) + "/** " + p.getDesc() + " Editor */");
            ps.println(indent(4) + "protected JComponent " + p.getName() + "Editor;");
            ps.println();

            switch(getEditor(p)) {
                case FILE_DIALOG:
                    ps.println(indent(4) + "protected JButton " + p.getName() + "Button;");
                    ps.println(indent(4) + "protected JFileChooser " + p.getName() + "Dialog;");
                    break;
            }
        }

        ps.println();
    }

    private String indent(int spaces) {
        Integer key = new Integer(spaces);

        String ret_val = indentCache.get(key);

        if (ret_val == null) {
            ret_val = "";
            for(int i=0; i < spaces; i++) {
                ret_val = ret_val + " ";
            }

            indentCache.put(key, ret_val);
        }

        return ret_val;
    }

    /**
     * Get the editor type to use.
     */
    private Editors getEditor(Parameter p) {
        if (p.getEditorType() == Parameter.EditorType.FILE_DIALOG) {
            return Editors.FILE_DIALOG;
        }

        if (p.getDataType() == Parameter.DataType.ENUM) {
            return Editors.COMBOBOX;
        }

        if (p.getDataType() == Parameter.DataType.BOOLEAN) {
            return Editors.COMBOBOX;
        }

        return Editors.TEXTFIELD;
    }
    
    private void setParams(PrintStream ps, Map<String,Parameter> params, Set<String> remove) {
    	Iterator<Parameter> itr = params.values().iterator();

        while(itr.hasNext()) {
            Parameter p = itr.next();
            if (remove.contains(p.getName())) {
                continue;
            }

            switch(getEditor(p)) {
                case TEXTFIELD:
                    ps.println(indent(12) + p.getName() + " = ((JTextField)" + p.getName() + "Editor).getText();");
                    break;
                case FILE_DIALOG:
                    ps.println(indent(12) + p.getName() + " = ((JTextField)" + p.getName() + "Editor).getText();");
                    break;
                case COMBOBOX:
                    ps.println(indent(12) + p.getName() + " = (String) ((JComboBox)" + p.getName() + "Editor).getSelectedItem();");
                    break;
                default:
                    System.out.println("Unhandled action for editor: " + getEditor(p));
            }

        }

        ps.println();

        // Create Kernel
        String class_name = kernel.getClass().getName();
        ps.println(indent(12) + class_name + " kernel = new " + class_name + "();");

        // Put params into a map

        ps.println(indent(12) + "HashMap<String,String> params = new HashMap<String,String>();");
        itr = params.values().iterator();

        while(itr.hasNext()) {
            Parameter p = itr.next();
            if (remove.contains(p.getName())) {
                continue;
            }

            ps.println(indent(12) + "params.put(\"" + p.getName() + "\", " + p.getName() + ");");
        }

        ps.println(indent(12) + "Map<String,Object> parsed_params = ParameterUtil.parseParams(kernel.getParams(), params);");
    }
    
    private void gatherParams(PrintStream ps, Map<String,Parameter> params, Set<String> remove) {
    	ps.println(indent(16) + "String properties = \"\";");
    	ps.println(indent(16) + "String val = null;");
    	
    	Iterator<Parameter> itr = params.values().iterator();

        while(itr.hasNext()) {
            Parameter p = itr.next();
            if (remove.contains(p.getName())) {
                continue;
            }

            switch(getEditor(p)) {
                case TEXTFIELD:
                	ps.println(indent(16) + "val = ((JTextField)" + p.getName() + "Editor).getText().replaceAll(\"\\\\\\\\+\", \"/\");");
                    ps.println(indent(16) + "properties += \"" + p.getName() + "=\"" + " + val + \"\\n\";");
                    break;
                case FILE_DIALOG:
                	ps.println(indent(16) + "val = ((JTextField)" + p.getName() + "Editor).getText().replaceAll(\"\\\\\\\\+\", \"/\");");
                    ps.println(indent(16) + "properties += \"" + p.getName() + "=\"" + " + val + \"\\n\";");
                    break;
                case COMBOBOX:
                    ps.println(indent(16) + "properties += \"" + p.getName() + "=\"" + " + (String) ((JComboBox)" + p.getName() + "Editor).getSelectedItem() + \"\\n\";");
                    break;
                default:
                    System.out.println("Unhandled action for editor: " + getEditor(p) + "\"");
            }
        }
    }
    
}
