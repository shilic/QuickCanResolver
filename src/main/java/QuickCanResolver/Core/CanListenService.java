package QuickCanResolver.Core;

public interface CanInputService {
    void onListen(int canId, byte[] data8);
}
