package wtf.rania.util.misc;

import wtf.rania.Client;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

    public static Gson gson = new Gson();

    public static String readInputStream(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line).append('\n');

        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public static File getFile(String filePath) {
        return new File(FileUtil.getRunningPath() + "/" + filePath);
    }

    public static String getString(String filePath) {
        return FileUtil.getRunningPath() + "/" + filePath;
    }

    public static Path getRunningPath() {
        return Paths.get(Minecraft.getMinecraft().mcDataDir.getPath(), Client.INSTANCE.getName());
    }

    public static void createFolder(String name) {
        try {
            Files.createDirectories(Paths.get(name));
        } catch (IOException exception) {
        }
    }
}