package de.hoffmann90.gratis.in.berlin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;

public class MainActivity extends SherlockFragmentActivity {

	public static final String EXTRA_URL = "de.hoffmann90.gratis.in.berlin.URL";

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	private class RetrievePage extends AsyncTask<String, Void, Elements> {

		private Exception exception;

		private Elements text;

		protected void onPostExecute(Elements t) {

			// TODO: check this.exception
			// TODO: do something with the feed
			if (this.exception != null) {
				this.exception.printStackTrace();
			} else
				text = t;
		}

		@Override
		protected Elements doInBackground(String... urls) {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(urls[0]);
			System.out.println(urls[0]);
			ResponseHandler<String> resHandler = new BasicResponseHandler();
			try {
				String html = httpClient.execute(httpGet, resHandler);
				Document doc = Jsoup.parse(html);

				Elements elements = doc.getElementById("showleft")
						.select("b a");
				// System.out.println(text.get(1).text());
				// List<String> result = new ArrayList<String>();
				// for (Element element : text) {
				// result.add(element.text());
				// }
				return elements;
			} catch (Exception e) {
				this.exception = e;
				return null;
			}
		}
	}

	static List<List<Event>> items;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Create the adapter that will return a fragment for each of the three
		// primary sections
		// of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		Calendar c = Calendar.getInstance();
		int month = c.get(Calendar.MONTH);
		int year = c.get(Calendar.YEAR);
		
		
		String[] urls = { "http://www.gratis-in-berlin.de/heute",
				"http://www.gratis-in-berlin.de/morgen",
				"http://www.gratis-in-berlin.de/uebermorgen", 
				"http://www.gratis-in-berlin.de/monat/" + (month + 1) + "." + year};

		items = new ArrayList<List<Event>>();

		for (String url : urls) {
			AsyncTask<String, Void, Elements> pageTask = new RetrievePage()
					.execute(url);

			List<Event> events = new ArrayList<Event>();
			try {
				Elements text = pageTask.get();
				
				
				for (Element element : text) {
					Event event = new Event();
					event.setTitle(element.text());
					event.setUrl(element.attr("href"));
					events.add(event);
				}

				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Context context = getApplicationContext();
				CharSequence toastText = "Fehler beim Abrufen der Seite. Bitte überprüfe deine Internetverbindung.";
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, toastText, duration);
				toast.show();
			}
			
			items.add(events);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			Fragment fragment = new DummySectionFragment();
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, i + 1);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			return 4;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Calendar c = Calendar.getInstance();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase();
			case 1:
				return getString(R.string.title_section2).toUpperCase();
			case 2:
				return getString(R.string.title_section3).toUpperCase();
			case 3:
				return c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMAN)
						.toUpperCase();
//			case 4:
//				return DateUtils.getMonthString(c.get(Calendar.MONTH) + 1, DateUtils.LENGTH_LONG)
//						.toUpperCase();
			}
			return null;
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		public DummySectionFragment() {
		}

		public static final String ARG_SECTION_NUMBER = "section_number";

		private ArrayList<Event> events;

		class EventAdapter extends BaseAdapter implements OnItemClickListener {
			private final LayoutInflater mInflater;

			public EventAdapter() {
				mInflater = (LayoutInflater) getActivity().getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
			}

			public int getCount() {
				return events.size();
			}

			public Event getItem(int position) {
				return events.get(position);
			}

			public long getItemId(int position) {
				return (long) position;
			}

			public View getView(int position, View convertView, ViewGroup parent) {
				LinearLayout itemView = (LinearLayout) mInflater.inflate(
						R.layout.event_list_item, parent, false);
				bindView(itemView, position);
				return itemView;
			}

			private void bindView(LinearLayout view, int position) {
				Event event = getItem(position);
				view.setId((int) getItemId(position));
				TextView titleTextView = (TextView) view
						.findViewById(R.id.title);
				titleTextView.setText(event.title);
//				TextView dateTextView = (TextView) view
//						.findViewById(R.id.date);
//				dateTextView.setText("15.10.2012");
			}

			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Event event = getItem(position);

				Intent intent = new Intent(getActivity(), EventDetail.class);
				String message = event.getUrl();
				intent.putExtra(EXTRA_URL, message);
				startActivity(intent);
			}
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			ListView listView = new ListView(getActivity());

			Bundle args = getArguments();
			events = new ArrayList<Event>();
			events = (ArrayList<Event>) items.get(args
					.getInt(ARG_SECTION_NUMBER) - 1);

			EventAdapter eventAdapter = new EventAdapter();

			// Assign adapter to ListView
			listView.setAdapter(eventAdapter);
			listView.setOnItemClickListener(eventAdapter);
			listView.setPadding(5, 0, 5, 0);
			return listView;
		}
	}
}