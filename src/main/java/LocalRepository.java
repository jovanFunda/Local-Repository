import com.google.gson.Gson;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class LocalRepository extends Repository {

    private Gson gson;
    private static File jsonFile;
    private static File configFile;

    static {
        RepositoryManager.setRepository(new LocalRepository());
    }

    @Override
    void AddUser(String username, String password, String priv) throws NoPrivilegeException, OnlyOneAdminUserCanExistException, UserAlreadyExistsException {

        Privilege privilege = null;
        if(priv.equals("admin")) {
            privilege = Privilege.ADMIN;
        } else if(priv.equals("user")) {
            privilege = Privilege.USER;
        } else if(priv.equals("spectator")) {
            privilege = Privilege.SPECTATOR;
        } else if(priv.equals("moderator")) {
            privilege = Privilege.MODERATOR;
        }

        for (User u : users) {
            if (u.getUsername().equals(username))
                throw new UserAlreadyExistsException();
        }

        if(privilege == Privilege.ADMIN)
            throw new OnlyOneAdminUserCanExistException();

        if(priv.equals("adminPermission")) {
            privilege = Privilege.ADMIN;
        }

        User user = new User(username, password, privilege);

        String json = gson.toJson(user);

        users.add(user);

        try
        {
            FileWriter writer = new FileWriter(jsonFile ,true);
            writer.append("\n" + json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    void CreateDirectory(String s) {
        File dir = new File(currentDirectory + "\\" + s);
        dir.mkdir();
    }

    @Override
    public void parseDirs(String s) {
        String temp = s;
        String[] nameTemp = temp.split("\\(");
        String dirName = nameTemp[0];

        String[] iTemp = nameTemp[1].split(",");
        int i = Integer.parseInt(iTemp[0]);

        String[] nTemp = iTemp[1].split("\\)");
        int n = Integer.parseInt(nTemp[0]);

        for( int j = i; j < n + 1; j++) {
            createDirectoriesWithCommonParent(currentDirectory, dirName + Integer.toString(j));
        }
    }

    public boolean createDirectoriesWithCommonParent(String parentString, String subs) {
        File parent = Paths.get(parentString).toFile();

        parent.mkdirs();
        if (!parent.exists() || !parent.isDirectory()) {
            return false;
        }


        File subFile = new File(parent, subs);
        subFile.mkdir();
        if (!subFile.exists() || !subFile.isDirectory()) {
            return false;
        }


        return true;
    }

    @Override
    void CreateFile(String s) {
        try {
            File file = new File(currentDirectory + "\\" + s);
            file.createNewFile();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    @Override
    void ListFiles() {
    }

    @Override
    void MoveFile(String origin, String destination) {
        try {
            Path temp = Files.move(Paths.get(origin), Paths.get(destination));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    void DeleteFile(String fileName) {
        try {
            Files.deleteIfExists(Paths.get(currentDirectory + "\\" + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void ExcludeExtension(String extension) {

        try {
            boolean exists = false;
            try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
                String line;
                while (true) {
                    if (!((line = br.readLine()) != null)) break;
                    if (line.equals("no_extension ." + extension)) {
                        exists = true;
                    }
                }
            }

            if (!exists) {
                Writer output = new BufferedWriter(new FileWriter(configFile, true));  //clears file every time
                output.append("no_extension ." + extension + "\n");
                output.close();
                excludedExtensions.add(extension);
            }

        /*for(String ex : excludedExtensions) {
            if(configFile.getAbsolutePath().endsWith(ex)) {
                throw new ExtensionException();
            }
        }*/
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void SetMaxBytes(int size) {
        try {
            List<String> out = Files.lines(configFile.toPath())
                    .filter(linija -> !linija.contains("max_size"))
                    .collect(Collectors.toList());
            Files.write(configFile.toPath(), out, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            Writer output = new BufferedWriter(new FileWriter(configFile, true));
            output.append("max_size " + size + "\n");
            output.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void SetMaxFoldersInDirectory(String path, int size) {
        try {
            List<String> out = Files.lines(configFile.toPath())
                    .filter(linija -> !(linija.split(" ")[0].equals("max_folders_in_dir") && linija.split(" ")[1].equals(path)))
                    .collect(Collectors.toList());
            Files.write(configFile.toPath(), out, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            Writer output = new BufferedWriter(new FileWriter(configFile, true));
            output.append("max_folders_in_dir " + path + " " + size + "\n");
            output.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long RepositorySize() {
        try {
            Path folder = Paths.get(rootDirectory);
            long size = Files.walk(folder)
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();

            return size;
        } catch(IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    boolean Init(String root) {
        boolean returnValue = false;
        users = new ArrayList<User>();
        gson = new Gson();
        jsonFile = new File(root + "\\Users.json");

        CreateDirectory(root);
        rootDirectory = root;
        currentDirectory = rootDirectory;

        try {

            if(jsonFile.exists()) {
                try {
                    BufferedReader buffer = new BufferedReader(new FileReader(jsonFile));
                    String line = buffer.readLine();

                    while (line != null){
                        users.add(gson.fromJson(line, User.class));
                        line = buffer.readLine();
                    }

                } catch(Exception e) {
                    e.printStackTrace();
                }
                returnValue = false;

            } else {

                FileWriter writer = new FileWriter(jsonFile);
                writer.close();
                returnValue = true;

            }

            File file = new File(rootDirectory + "\\config.txt");
            excludedExtensions = new HashSet<>();

            if(file.isFile()) {
                configFile = file;
            } else {
                file.createNewFile();
                configFile = file;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnValue;
    }
}
