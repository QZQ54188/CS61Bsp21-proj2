package gitlet;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static gitlet.Utils.*;
import static gitlet.Repository.OBJECT_DIR;


public class Blob implements Serializable{
    private String blobID;

    private byte[] contents;

    private File blobSaveFile;

    private String filePath;

    private File fileName;

    public Blob(File fileName){
        this.fileName = fileName;
        this.contents = readContents(fileName);
        this.filePath = fileName.getPath();
        this.blobID = generateBlobID();
        this.blobSaveFile = generateBlobSaveFile();
    }

    public void save(){
        writeContents(blobSaveFile, this);
    }

    /**Calculate sha1 hash value based on file path and file content*/
    private String generateBlobID(){
        return sha1(filePath, contents);
    }

    private File generateBlobSaveFile(){
        return join(OBJECT_DIR, blobID);
    }

    public String getBlobID(){
        return blobID;
    }

    public String getFilePath(){
        return filePath;
    }

    public byte[] getContents(){
        return contents;
    }

    public File getBlobSaveFile(){
        return blobSaveFile;
    }

    public String getFileName(){
        return fileName.getName();
    }
}
