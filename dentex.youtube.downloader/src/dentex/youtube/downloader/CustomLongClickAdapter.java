package dentex.youtube.downloader;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/* method addId3Tags adapted from Stack Overflow:
 * 
 * http://stackoverflow.com/questions/11300381/disabled-listitems-in-alertdialog-does-not-show-up-as-grayed-out-items
 * 
 * Q: http://stackoverflow.com/users/849664/chirag-shah
 * A: http://stackoverflow.com/users/903469/mkjparekh
 */

public class CustomLongClickAdapter extends ArrayAdapter<CharSequence> {

    static int disabledOption = 0;

    private CustomLongClickAdapter(Context context, int textViewResId, CharSequence[] strings, int disabledOption) {
        super(context, textViewResId, strings);
        CustomLongClickAdapter.disabledOption = disabledOption;
    }

    public static CustomLongClickAdapter createFromResource(Context context, int textViewResId,
            boolean disableOptionA) {

        Resources resources = context.getResources();
        CharSequence[] strings = resources.getTextArray(R.array.dashboard_long_click_entries);

        return new CustomLongClickAdapter(context, textViewResId, strings, disabledOption);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        view.setEnabled(isEnabled(position));
        return view;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        if (position == disabledOption) {
        	return false;
        }
        return true;
    }
}
