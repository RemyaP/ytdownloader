package dentex.youtube.downloader.docs;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import dentex.youtube.downloader.R;

public class ShowBssActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_bss);
         
        // Get the AssetManager
        AssetManager manager = getAssets();
 
        // Read a Bitmap from Assets
        try {
        	InputStream open = manager.open("bss.png");
            Bitmap bitmap = BitmapFactory.decodeStream(open);
            // Assign the bitmap to an ImageView in this layout
            ImageView view = (ImageView) findViewById(R.id.imgview1);
            view.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
