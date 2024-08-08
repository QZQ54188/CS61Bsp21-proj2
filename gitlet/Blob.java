package gitlet;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static gitlet.Utils.*;
import static gitlet.Repository.OBJECT_DIR;


public class Blob implements Serializable{

    /**Basic information about Blob files*/
    private String blobID;
    private byte[] contents;
    private String filePath;

    private String fileName;

    private File blobSaveFile;

    public Blob(File fileName){
        this.fileName = fileName.getName();
        this.contents = readContents(fileName);
        this.filePath = fileName.getPath();

        this.blobID = generateBlobID();
        this.blobSaveFile = generateBlobSaveFile();
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
        return fileName;
    }

    public void save(){
        writeContents(blobSaveFile, this);
    }
}
