import java.io.File;

public class LocalRepository extends Repository {

    static {
        RepositoryManager.setRepository(new LocalRepository("repository"));
    }

    protected LocalRepository(String rootDirectory) {
        super(rootDirectory);
    }

    @Override
    int CreateDirectory(String s) {
        System.out.println("Pozdrav!");
        return 0;
    }

    @Override
    int CreateFile(String s) {
        return 0;
    }

    @Override
    int ListFiles() {
        return 0;
    }

    @Override
    int MoveFile(File file, String s) {
        return 0;
    }

    @Override
    int DeleteFile(File file) {
        return 0;
    }

    @Override
    int Init() {
        return 0;
    }
}
