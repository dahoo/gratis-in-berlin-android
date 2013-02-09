package de.hoffmann90.gratis.in.berlin;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class EventDetail extends SherlockActivity {

	String title, date, details, location, link, url;

	private class RetrievePage extends AsyncTask<String, Void, Element> {

		private Exception exception;
		private EventDetail activity;

		@Override
		protected void onPostExecute(Element t) {
			if (this.exception != null) {
				this.exception.printStackTrace();
				activity.handleError(exception);
			} else {
			}
		}
		
		RetrievePage attach(EventDetail activity) {
		      this.activity=activity;
		      return this;
		    }

		@Override
		protected Element doInBackground(String... urls) {			
			Document doc;
			try {
				doc = Jsoup.connect(url).get();
		        Element elementsHtml = doc.getElementById("gib_tip");
				return elementsHtml;
			} catch (IOException e) {
				e.printStackTrace();
			}
	        return null;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_event_detail);

		// Get the message from the intent
		Intent intent = getIntent();
		url = intent.getStringExtra(MainActivity.EXTRA_URL);

		AsyncTask<String, Void, Element> pageTask = new RetrievePage()
			.attach(this).execute(url);

		Element text = null;
		try {
			text = pageTask.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(text == null)
			return;			

		Elements itemProps = text.select("[itemprop]");
		title = itemProps.select("[itemprop=name]").text();
		SimpleDateFormat dateParser=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		SimpleDateFormat germanDateFormat = new SimpleDateFormat("dd.MM.yyyy");
		try {
			String dateString = itemProps.select("[itemprop=startDate]").first().attr("content");
			date =  germanDateFormat.format(dateParser.parse(dateString));
		} catch (Exception e1) {
			e1.printStackTrace();
			date = "";
		}
		//TODO: Get end date (if available)
		details = itemProps.select("[itemprop=description]").text().trim();
		location = itemProps.select("[itemprop=location]").text().replace(" - zum Stadplan", "").trim();
		String linkHTML;
		try {
			link = text.select("div.urlInfo a").first().attr("href");
			linkHTML = text.select("div.urlInfo a").first().outerHtml();
		} catch (NullPointerException e) {
			link = "";
			linkHTML = "";
		}
		String admission = "";

		TextView titleText = (TextView) findViewById(R.id.textViewTitle);
		titleText.setText(title);

		TextView dateText = (TextView) findViewById(R.id.textViewDate);
		dateText.setText(date);

		TextView admissionText = (TextView) findViewById(R.id.textViewAdmission);
		admissionText.setText(admission);

		TextView locationText = (TextView) findViewById(R.id.textViewLocation);
		locationText.setText(location);
		Drawable img = this.getResources().getDrawable(
				android.R.drawable.ic_dialog_map);
		img.setBounds(0, 0, 60, 60);
		locationText.setCompoundDrawablesWithIntrinsicBounds(img, null, null,
				null);
		locationText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String uri = String.format("geo:0,0?q=%s", location);
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
				startActivity(intent);
			}
		});

		TextView detailsText = (TextView) findViewById(R.id.textViewDetails);
		detailsText.setText(details);

		TextView linkText = (TextView) findViewById(R.id.textViewLink);
		linkText.setText(Html.fromHtml(linkHTML));
		linkText.setMovementMethod(LinkMovementMethod.getInstance());
	}
		
	private void handleError(Throwable t) {
		Context context = getApplicationContext();
		CharSequence toastText = "Fehler.";
		if (t instanceof IllegalArgumentException) {
			toastText = "Ungültiges Zeichen in URL.";
		} else {
			toastText = "Fehler beim Abrufen der Seite.";
		}
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(context, toastText, duration);
		toast.show();
		NavUtils.navigateUpFromSameTask(this);
	  }
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_event_detail, menu);

		if (Build.VERSION.SDK_INT < 14)
			menu.removeItem(R.id.add_event);
		return true;
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.add_event:
			if (Build.VERSION.SDK_INT >= 14) {
				Intent calIntent = new Intent(Intent.ACTION_INSERT);
				calIntent.setData(CalendarContract.Events.CONTENT_URI);
				calIntent.putExtra(Events.TITLE, title);
				calIntent.putExtra(Events.EVENT_LOCATION, location);
				String description = details;
				if (!link.equals("")) {
					description += "\n\n" + link;
				}
				description += "\n\n" + url;
				calIntent.putExtra(Events.DESCRIPTION, description);
				
				Calendar beginCalDate;
				Calendar endCalDate;
				if(date == "Dauerbrenner") {
					beginCalDate = Calendar.getInstance();
					endCalDate = beginCalDate;
				}
				else {
					String[] dates = date.split("bis");
					String fromDate = dates[0].trim();
					System.out.println(fromDate);
					String[] fromDateParts = fromDate.split("\\.");
					beginCalDate = new GregorianCalendar(
							Integer.parseInt(fromDateParts[2]),
							Integer.parseInt(fromDateParts[1]) - 1,
							Integer.parseInt(fromDateParts[0]));
					
					if(dates.length > 1) {
						String toDate = dates[1].trim();
						System.out.println(toDate);
						String[] toDateParts = toDate.split("\\.");
						endCalDate = new GregorianCalendar(
								Integer.parseInt(toDateParts[2]),
								Integer.parseInt(toDateParts[1]) - 1,
								Integer.parseInt(toDateParts[0]) + 1);
					}
					else {
						endCalDate = beginCalDate;
					}
				}
				
				calIntent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);
				calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
						beginCalDate.getTimeInMillis());
				
				calIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
						endCalDate.getTimeInMillis());
				startActivity(calIntent);
			}
			return true;
			
		case R.id.open_in_browser:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(browserIntent);

		}
		return super.onOptionsItemSelected(item);
	}

}
