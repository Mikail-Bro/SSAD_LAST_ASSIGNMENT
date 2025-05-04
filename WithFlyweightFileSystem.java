// All needed imports
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

// Main class to run the program
public class WithFlyweightFileSystem {
    public static void main(String[] args) {
        Locale.setDefault(Locale.US); // Make sure that decimal separator is dot
        FileSystemFacade.AnalyzeInput(); // Start processing input
    }
}

// FACADE DESIGN PATTERN
// Provides simplified interface to complex subsystems
class FileSystemFacade {
    private final Directory theMainRoot = new Directory("root"); // Root directory
    private final Map<Integer, Directory> directoryMap = new HashMap<>(); // Map of directories
    private final FilePropertiesFactory factory = new FilePropertiesFactory(); // Factory for file properties

    public FileSystemFacade() {
        directoryMap.put(0, theMainRoot); // Initialize root directory
    }

    // Process input commands from user
    public static void AnalyzeInput() {
        Scanner reader = new Scanner(System.in);
        int n = reader.nextInt(); // Number of commands
        reader.nextLine(); // Consume newline
        FileSystemFacade fileSystemManager = new FileSystemFacade();

        for (int i = 0; i < n; i++) {
            String command = reader.next();
            if (command.equals("DIR")) {
                int id = reader.nextInt();
                int parent_id = 0;
                String name;
                if (reader.hasNextInt()) { // If parent ID exists
                    parent_id = reader.nextInt();
                    name = reader.next();
                } else { // No parent ID provided
                    name = reader.next();
                }
                fileSystemManager.addDirectory(id, parent_id, name); // Add directory
            } else if (command.equals("FILE")) {
                int parent_id = reader.nextInt();
                boolean readOnly = reader.next().equals("T");
                String owner = reader.next();
                String group = reader.next();
                double size = reader.nextDouble();
                String filename = reader.next();

                String[] parts = filename.split("\\.");
                String extension = "";
                if (parts.length == 2 && !parts[1].isEmpty() && !parts[0].isEmpty()) {
                    extension = parts[1];
                }
                fileSystemManager.addFile(parent_id, readOnly, owner, group, size, filename, extension);
            }
            reader.nextLine(); // Consume remaining newline
        }

        fileSystemManager.displayHierarchy(); // Display tree and total size
        reader.close();
    }

    // Add a new directory to the map and its parent
    public void addDirectory(int i_d, int pa_id, String n_e) {
        Directory newDirectory = new Directory(n_e);
        directoryMap.put(i_d, newDirectory);
        directoryMap.get(pa_id).add(newDirectory);
    }

    // Add a new file to the specified parent directory
    public void addFile(int pa_id, boolean re_ly, String ow_er, String gr_up, double s_e, String n_e, String e_n) {
        FileProperties newFileProperties = factory.get(e_n, re_ly, ow_er, gr_up);
        File newFile = new File(n_e, newFileProperties, s_e);
        directoryMap.get(pa_id).add(newFile);
    }

    // Display the file system hierarchy and total size
    public void displayHierarchy() {
        SizeVisitor someVisitor = new SizeVisitor();
        theMainRoot.accept(someVisitor);

        DecimalFormat format = new DecimalFormat("0.#");
        String formattedSize = format.format(someVisitor.getTotalSize());

        System.out.println("total: " + formattedSize + "KB");
        theMainRoot.display("", true);
    }
}

// Base class for all file system nodes (files and directories)
abstract class Node {
    public abstract void accept(Visitor visitor);
    public abstract void display(String begin, boolean isLast);
    public abstract String getName();
    public abstract Iterator<Node> createIterator(); // Iterator<Node> has even more functionality that the required Iterator<Directory>
}

// Represents a file in the file system
class File extends Node {
    private final String name;
    private final FileProperties properties;
    private final double size;

    public File(String n_e, FileProperties p_s, double s_e) {
        this.name = n_e;
        this.properties = p_s;
        this.size = s_e;
    }

    public double getSize() {
        return size;
    }

    @Override
    public void accept(Visitor theVisitor) {
        theVisitor.visit(this); // Accept visitor
    }

    @Override
    public void display(String begin, boolean isLast) {
        DecimalFormat format = new DecimalFormat("0.#");
        String theSizeString = format.format(size);
        String output;
        if (isLast) {
            output = begin + "└── " + name;
        } else {
            output = begin + "├── " + name;
        }
        output += " (" + theSizeString + "KB)";
        output = output.substring(4); // Remove extra indent for root
        System.out.println(output);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Iterator<Node> createIterator() {
        return Collections.emptyIterator(); // Files have no children
    }
}

// Represents a directory that can contain files and subdirectories
class Directory extends Node {
    private final String name;
    private final List<Node> children = new ArrayList<>();

    public Directory(String name) {
        this.name = name;
    }

    public void add(Node node) {
        children.add(node);
    }

    @Override
    public void accept(Visitor theVisitor) {
        theVisitor.visit(this); // Visit this directory
        for (Node child : children) {
            child.accept(theVisitor); // Visit each child
        }
    }

    @Override
    public void display(String begin, boolean isLast) {
        if (!begin.isEmpty()) {
            String output;
            if (isLast) {
                output = begin + "└── " + name;
            } else {
                output = begin + "├── " + name;
            }
            output = output.substring(4); // Remove extra indent
            System.out.println(output);
        } else {
            System.out.println(".");
        }

        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            boolean last = (i == children.size() - 1);
            String newBegin = begin;
            
            if (isLast) {
                newBegin += "    ";
            } else {
                newBegin += "│   ";
            }
            
            child.display(newBegin, last); // Recursively display children
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Iterator<Node> createIterator() {
        return new DepthFirstIterator(this);
    }

    public List<Node> getChildren() {
        return children;
    }
}

// ITERATOR DESIGN PATTERN
// Implements depth-first traversal of file system nodes
class DepthFirstIterator implements Iterator<Node> {
    private final Stack<Iterator<Node>> iteratorsStack = new Stack<>();

    public DepthFirstIterator(Directory root) {
        iteratorsStack.push(root.getChildren().iterator());
    }

    @Override
    public boolean hasNext() {
        while (!iteratorsStack.isEmpty()) {
            if (iteratorsStack.peek().hasNext()) {
                return true;
            } else {
                iteratorsStack.pop(); // Pop empty iterator
            }
        }
        return false;
    }

    @Override
    public Node next() {
        Node theNode = iteratorsStack.peek().next();
        if (theNode instanceof Directory) {
            iteratorsStack.push(((Directory) theNode).getChildren().iterator()); // Traverse deeper
        }
        return theNode;
    }
}

// VISITOR DESIGN PATTERN
// Interface for visitors acting on file system nodes
interface Visitor {
    void visit(File file);
    void visit(Directory directory);
}

// Calculates total size of all files
class SizeVisitor implements Visitor {
    private double overallSize = 0;

    @Override
    public void visit(File file) {
        overallSize += file.getSize(); // Accumulate file size
    }

    @Override
    public void visit(Directory directory) {}

    public double getTotalSize() {
        return overallSize;
    }
}

// FLYWEIGHT DESIGN PATTERN
// Stores shared file metadata
class FileProperties {
    public final boolean readOnly;
    public final String group;
    public final String owner;
    public final String extension;

    public FileProperties(String ex_on, boolean re_ly, String ow_er, String gr_up) {
        this.extension = ex_on;
        this.readOnly = re_ly;
        this.owner = ow_er;
        this.group = gr_up;
    }

    @Override
    public boolean equals(Object node) {
        if (!(node instanceof FileProperties)) return false;
        FileProperties other = (FileProperties) node;
        return extension.equals(other.extension)
                && group.equals(other.group)
                && owner.equals(other.owner)
                && readOnly == other.readOnly;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.extension, this.readOnly, this.owner, this.group);
    }
}

// FLYWEIGHT DESIGN PATTERN
// Factory for creating or reusing FileProperties objects
class FilePropertiesFactory {
    private final Map<FileProperties, FileProperties> fileCache = new HashMap<>();

    public FileProperties get(String e_n, boolean r_y, String o_r, String g_p) {
        FileProperties key = new FileProperties(e_n, r_y, o_r, g_p);
        if (!fileCache.containsKey(key)) {
            fileCache.put(key, key); // Store new instance
        }
        return fileCache.get(key); // Return cached or new
    }
}