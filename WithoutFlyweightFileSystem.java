// All needed imports
import java.text.DecimalFormat;
import java.util.*;

public class WithoutFlyweightFileSystem {
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        FileSystemFacade.AnalyzeInput();
    }
}

class FileSystemFacade {
    private final Directory theMainRoot = new Directory("root");
    private final Map<Integer, Directory> directoryMap = new HashMap<>();

    public FileSystemFacade() {
        directoryMap.put(0, theMainRoot);
    }

    public static void AnalyzeInput() {
        Scanner reader = new Scanner(System.in);
        int n = reader.nextInt();
        reader.nextLine();
        FileSystemFacade fileSystemManager = new FileSystemFacade();

        for (int i = 0; i < n; i++) {
            String command = reader.next();
            if (command.equals("DIR")) {
                int id = reader.nextInt();
                int parent_id = 0;
                String name;
                if (reader.hasNextInt()) {
                    parent_id = reader.nextInt();
                    name = reader.next();
                } else {
                    name = reader.next();
                }
                fileSystemManager.addDirectory(id, parent_id, name);
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
            reader.nextLine();
        }

        fileSystemManager.displayHierarchy();
        reader.close();
    }

    public void addDirectory(int i_d, int pa_id, String n_e) {
        Directory newDirectory = new Directory(n_e);
        directoryMap.put(i_d, newDirectory);
        directoryMap.get(pa_id).add(newDirectory);
    }

    public void addFile(int pa_id, boolean readOnly, String owner, String group, double size, String name, String ext) {
        File newFile = new File(name, readOnly, owner, group, ext, size);
        directoryMap.get(pa_id).add(newFile);
    }

    public void displayHierarchy() {
        SizeVisitor someVisitor = new SizeVisitor();
        theMainRoot.accept(someVisitor);

        DecimalFormat format = new DecimalFormat("0.#");
        String formattedSize = format.format(someVisitor.getTotalSize());

        System.out.println("total: " + formattedSize + "KB");
        theMainRoot.display("", true);
    }
}

abstract class Node {
    public abstract void accept(Visitor visitor);
    public abstract void display(String begin, boolean isLast);
    public abstract String getName();
    public abstract Iterator<Node> createIterator();
}

class File extends Node {
    private final String name;
    private final boolean readOnly;
    private final String owner;
    private final String group;
    private final String extension;
    private final double size;

    public File(String name, boolean readOnly, String owner, String group, String extension, double size) {
        this.name = name;
        this.readOnly = readOnly;
        this.owner = owner;
        this.group = group;
        this.extension = extension;
        this.size = size;
    }

    public double getSize() {
        return size;
    }

    @Override
    public void accept(Visitor theVisitor) {
        theVisitor.visit(this);
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
        output = output.substring(4);
        System.out.println(output);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Iterator<Node> createIterator() {
        return Collections.emptyIterator();
    }
}

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
        theVisitor.visit(this);
        for (Node child : children) {
            child.accept(theVisitor);
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
            output = output.substring(4);
            System.out.println(output);
        } else {
            System.out.println(".");
        }

        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            boolean last = (i == children.size() - 1);
            String newBegin = begin + (isLast ? "    " : "│   ");
            child.display(newBegin, last);
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
                iteratorsStack.pop();
            }
        }
        return false;
    }

    @Override
    public Node next() {
        Node theNode = iteratorsStack.peek().next();
        if (theNode instanceof Directory) {
            iteratorsStack.push(((Directory) theNode).getChildren().iterator());
        }
        return theNode;
    }
}

interface Visitor {
    void visit(File file);
    void visit(Directory directory);
}

class SizeVisitor implements Visitor {
    private double overallSize = 0;

    @Override
    public void visit(File file) {
        overallSize += file.getSize();
    }

    @Override
    public void visit(Directory directory) {}

    public double getTotalSize() {
        return overallSize;
    }
}
