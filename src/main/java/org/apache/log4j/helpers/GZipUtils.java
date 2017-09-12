package org.apache.log4j.helpers;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * <p>Title: GZipUtils</p>
 * <p>Description: GZip compress & decompress</p>
 *
 * @author Victor
 * @version 1.3.2
 * @see GZIPInputStream
 * @see GZIPOutputStream
 * @since 2017 /9/11
 */
public abstract class GZipUtils {

    /**
     * The constant BUFFER.
     */
    public static final int BUFFER = 4 * 1024;
    /**
     * The constant EXT.
     */
    public static final String EXT = ".gz";

    /**
     * 数据压缩
     *
     * @param data the data
     * @return byte [ ]
     * @throws IOException the io exception
     */
    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 压缩
        compress(bais, baos);

        byte[] output = baos.toByteArray();

        baos.flush();
        baos.close();

        bais.close();

        return output;
    }

    /**
     * 文件压缩
     *
     * @param file the file
     * @throws IOException the io exception
     */
    public static void compress(File file) throws IOException {
        compress(file, true);
    }

    /**
     * 文件压缩
     *
     * @param file   the file
     * @param delete 是否删除原始文件
     * @throws IOException the io exception
     */
    public static void compress(File file, boolean delete) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        FileOutputStream fos = new FileOutputStream(file.getPath() + EXT);

        compress(fis, fos);

        fis.close();
        fos.flush();
        fos.close();

        if (delete) {
            file.delete();
        }
    }

    /**
     * 数据压缩
     *
     * @param is the is
     * @param os the os
     * @throws IOException the io exception
     */
    public static void compress(InputStream is, OutputStream os)
            throws IOException {

        GZIPOutputStream gos = new GZIPOutputStream(os, BUFFER);

        int count;
        byte data[] = new byte[BUFFER];
        while ((count = is.read(data, 0, BUFFER)) != -1) {
            gos.write(data, 0, count);
        }

        gos.finish();

        gos.flush();
        gos.close();
    }

    /**
     * 文件压缩
     *
     * @param path the path
     * @throws IOException the io exception
     */
    public static void compress(String path) throws IOException {
        compress(path, true);
    }

    /**
     * 文件压缩
     *
     * @param path   the path
     * @param delete 是否删除原始文件
     * @throws IOException the io exception
     */
    public static void compress(String path, boolean delete) throws IOException {
        File file = new File(path);
        compress(file, delete);
    }

    /**
     * 数据解压缩
     *
     * @param data the data
     * @return byte [ ]
     * @throws IOException the io exception
     */
    public static byte[] decompress(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 解压缩

        decompress(bais, baos);

        data = baos.toByteArray();

        baos.flush();
        baos.close();

        bais.close();

        return data;
    }

    /**
     * 文件解压缩
     *
     * @param file the file
     * @throws IOException the io exception
     */
    public static void decompress(File file) throws IOException {
        decompress(file, true);
    }

    /**
     * 文件解压缩
     *
     * @param file   the file
     * @param delete 是否删除原始文件
     * @throws IOException the io exception
     */
    public static void decompress(File file, boolean delete) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        FileOutputStream fos = new FileOutputStream(file.getPath().replace(EXT,
                ""));
        decompress(fis, fos);
        fis.close();
        fos.flush();
        fos.close();

        if (delete) {
            file.delete();
        }
    }

    /**
     * 数据解压缩
     *
     * @param is the is
     * @param os the os
     * @throws IOException the io exception
     */
    public static void decompress(InputStream is, OutputStream os)
            throws IOException {

        GZIPInputStream gis = new GZIPInputStream(is, BUFFER);

        int count;
        byte data[] = new byte[BUFFER];
        while ((count = gis.read(data, 0, BUFFER)) != -1) {
            os.write(data, 0, count);
        }

        gis.close();
    }

    /**
     * 文件解压缩
     *
     * @param path the path
     * @throws IOException the io exception
     */
    public static void decompress(String path) throws IOException {
        decompress(path, true);
    }

    /**
     * 文件解压缩
     *
     * @param path   the path
     * @param delete 是否删除原始文件
     * @throws IOException the io exception
     */
    public static void decompress(String path, boolean delete) throws IOException {
        File file = new File(path);
        decompress(file, delete);
    }

}
