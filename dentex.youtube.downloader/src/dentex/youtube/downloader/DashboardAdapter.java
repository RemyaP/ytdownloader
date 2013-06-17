package dentex.youtube.downloader;
/* 
 * code adapted from: 
 * https://github.com/survivingwithandroid/Surviving-with-android/tree/master/SimpleList
 * 
 * Copyright (C) 2012 jfrankie (http://www.survivingwithandroid.com)
 * Copyright (C) 2012 Surviving with Android (http://www.survivingwithandroid.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class DashboardAdapter extends ArrayAdapter<DashboardListItem> implements Filterable {

	private List<DashboardListItem> itemsList;
	private Context context;
	private Filter itemsFilter;
	private List<DashboardListItem> origItemsList;
	
	public DashboardAdapter(List<DashboardListItem> itemsList, Context ctx) {
		super(ctx, R.layout.activity_dashboard_list_item, itemsList);
		this.itemsList = itemsList;
		this.context = ctx;
		this.origItemsList = itemsList;
	}
	
	public int getCount() {
		return itemsList.size();
	}

	public DashboardListItem getItem(int position) {
		return itemsList.get(position);
	}

	public long getItemId(int position) {
		return itemsList.get(position).hashCode();
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		
		ItemHolder holder = new ItemHolder();
		
		// First let's verify the convertView is not null
		if (convertView == null) {
			// This a new view we inflate the new layout
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.activity_dashboard_list_item, null);
			// Now we can fill the layout with the right values
			TextView tv1 = (TextView) v.findViewById(R.id.one);
			TextView tv2 = (TextView) v.findViewById(R.id.two);
			TextView tv3 = (TextView) v.findViewById(R.id.three);
			TextView tv4 = (TextView) v.findViewById(R.id.four);

			holder.itemOne = tv1;
			holder.itemTwo = tv2;
			holder.itemThree = tv3;
			holder.itemFour = tv4;
			
			v.setTag(holder);
		} else {
			holder = (ItemHolder) v.getTag();
		}
		
		DashboardListItem dli = itemsList.get(position);
		holder.itemOne.setText(dli.getFilename());
		holder.itemTwo.setText(dli.getSize());
		holder.itemThree.setText(dli.getPath());
		holder.itemFour.setText(dli.getStatus());
		
			//holder.itemFour.setTextColor(Color.GREEN);
			//holder.itemFour.setTextColor(Color.RED);
		
		return v;
	}
	
	/* *********************************
	 * We use the holder pattern        
	 * It makes the view faster and avoid finding the component
	 * **********************************/
	
	private static class ItemHolder {
		public TextView itemOne;
		public TextView itemTwo;
		public TextView itemThree;
		public TextView itemFour;
	}

	public class PlanetFilter extends Filter {
	    @Override
	    public FilterResults performFiltering(CharSequence constraint) {
	    	FilterResults results = new FilterResults();
	        // We implement here the filter logic
	        if (constraint == null || constraint.length() == 0) {
	            // No filter implemented we return all the list
	            results.values = origItemsList;
	            results.count = origItemsList.size();
	        } else {
	            // We perform filtering operation
	            List<DashboardListItem> nList = new ArrayList<DashboardListItem>();
	             
	            for (DashboardListItem p : itemsList) {
	                if (p.getFilename().startsWith(constraint.toString()))
	                    nList.add(p);
	            }
	             
	            results.values = nList;
	            results.count = nList.size();
	        }
	        return results;
	    }
	 
	    @SuppressWarnings("unchecked")
		@Override
	    public void publishResults(CharSequence constraint,FilterResults results) {
	    	// Now we have to inform the adapter about the new list filtered
	        if (results.count == 0) {
	            notifyDataSetInvalidated();
	        } else {
	        	itemsList = (List<DashboardListItem>) results.values;
	            notifyDataSetChanged();
	        }
	    }
	}
	
	@Override
	public Filter getFilter() {
	    if (itemsFilter == null)
	        itemsFilter = new PlanetFilter();
	     
	    return itemsFilter;
	}
	
	public void resetData() {
		itemsList = origItemsList;
	}
	
}

