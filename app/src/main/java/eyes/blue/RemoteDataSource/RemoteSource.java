package eyes.blue.RemoteDataSource;

public interface RemoteSource {
	public abstract String getName();
	public abstract String getMediaFileAddress(int i);
	public abstract String getSubtitleFileAddress(int i);
	public abstract String getTheoryFileAddress(int i);
	public abstract String getGlobalLamrimSchedule();
}
