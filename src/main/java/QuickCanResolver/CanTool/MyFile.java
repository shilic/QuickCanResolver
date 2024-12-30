package QuickCanResolver.CanTool;

import java.io.File;
import java.io.IOException;

public class MyFile {
    /**
     * 根据文件绝对路径，创建一个不重复的文件。如果文件名称不存在则直接创建；如果存在，则重命名一个新的文件并作为返回值
     * @param filePath 文件绝对路径
     * @return 如果创建成功，返回File对象，创建失败返回 null
     */
    public static File newFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) { //判断文件是否存在
            System.out.println("文件存在，重命名新创建的文件");
            String newFilePath = newFilePath(file); //生成新的文件绝对路径
            file = newFile(newFilePath); //函数嵌套调用自身，再次判断是否文件名重复,循环，直到进入下边的else语句
            return file;
        } else {  //如果文件不存在才会创建新文件
            System.out.println("文件不存在！系统创建新文件");
            try {
                if (file.createNewFile()) {
                    ///System.out.println("新文件创建成功！");
                    return file;
                } else {
                    ///createNewFile()方法在创建新文件的时候，如果文件已经存在，也是无法创建的,会抛出异常
                    ///System.out.println("新文件创建失败！");
                    return null;
                }
            } catch (IOException e) {
                System.out.println("文件操作发生异常：" + e.getMessage());
                return null;
            }
        }
    }

    /** 得到一个不重复的文件链接 */
    public static String newFilePath(String filePath) {
        File file = new File(filePath);
        if (file.exists()) { //判断文件是否存在
            System.out.println("文件存在，重命名新创建新的文件链接");
            String newFilePath = newFilePath(file);
            newFilePath = newFilePath(newFilePath); //函数嵌套调用自身，再次判断是否文件名重复,循环，直到进入下边的else语句
            return newFilePath;
        } else {  //如果文件不存在
            System.out.println("文件不存在！系统创建新的文件链接");
            return filePath;
        }
    }
    private static String newFilePath(File file){
        String parentPath = file.getParent(); //获取文件所在文件夹
        String fileName = file.getName();  //文件名
        //substring方法获取文件扩展名 ，以及文件名（不含扩展名）
        String fileExtension = fileName.substring(fileName.lastIndexOf(".")); //文件后缀(包含点)
        String fileWithoutEX = fileName.substring(0,fileName.lastIndexOf(".")); //获取下标0到最后一个点之前的文件名

        String newFileName = fileWithoutEX+"(new)" + fileExtension; //生成新的文件名

        return parentPath+"\\"+newFileName;
    }
}
