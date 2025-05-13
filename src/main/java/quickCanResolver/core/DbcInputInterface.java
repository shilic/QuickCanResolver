package quickCanResolver.core;

import java.io.InputStream;

/**
 * DBC输入接口，因为不同平台的输入输出环境不一致，所以不能直接使用相对路径或是绝对路径，故定义接口。由外部使用者自行实现。
 */
public interface DbcInputInterface {
    /**
     * DBC输入接口。为了兼容不同平台的文件输入方式。使用者按需实现即可。
     * @param dbcFilePath Assets 文件夹里文件的相对路径，或者其他方式的相对路径
     * @return 返回一个输入流
     */
    InputStream getInputStream(String dbcFilePath);
}
