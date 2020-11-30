package id.ac.ui.cs.mobileprogramming.razaqadhafin.topik9;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface FileDownloadClient {

    @Streaming
    @GET
    Call<ResponseBody> downloadFileStream(@Url String url);
}
