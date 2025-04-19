package QuickCanResolver.Core;

public interface CanListenService {
    void listened(int canId, byte[] data8);
}
