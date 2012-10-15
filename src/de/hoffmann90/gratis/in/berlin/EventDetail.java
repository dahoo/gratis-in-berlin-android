package de.hoffmann90.gratis.in.berlin;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

	String title, date, details, location, link;

	private class RetrievePage extends AsyncTask<String, Void, Elements> {

		private Exception exception;

		protected void onPostExecute(Elements t) {

			// TODO: check this.exception
			// TODO: do something with the feed
			if (this.exception != null) {
				this.exception.printStackTrace();
			} else {
			}
		}

		@Override
		protected Elements doInBackground(String... urls) {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(urls[0]);
			ResponseHandler<String> resHandler = new BasicResponseHandler();
			try {
				String html = httpClient.execute(httpGet, resHandler);
				Document doc = Jsoup.parse(html);

				Elements elements = doc.select("div#showleft");
				return elements;
			} catch (Exception e) {
				this.exception = e;
				return null;
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_event_detail);

		// Get the message from the intent
		Intent intent = getIntent();
		String message = intent.getStringExtra(MainActivity.EXTRA_URL);

		AsyncTask<String, Void, Elements> pageTask = new RetrievePage()
				.execute(message);

		Elements text = null;
		try {
			text = pageTask.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(text == null) {
			NavUtils.navigateUpFromSameTask(this);
			Context context = getApplicationContext();
			CharSequence toastText = "Fehler beim Abrufen der Seite. Bitte überprüfe deine Internetverbindung.";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(context, toastText, duration);
			toast.show();
			return;
		}

		title = text.select("div#eventHeadline").text();
		date = text.select("div#eventText b").first().text();
		details = text.select("div#eventText").text();
		String linkHTML = text.select("div#eventURL a").outerHtml();
		try {
			link = text.select("div#eventURL a").first().attr("href");
		} catch (NullPointerException e) {
			link = "";
		}
		String admission = "";

		Pattern pattern = Pattern
				.compile("Kostenlos(.*Einschränkung:.*</div>)?");
		Matcher matcher = pattern.matcher(text.select("div#eventText").html());
		// Check all occurance
		List<String> matches = new ArrayList<String>();
		while (matcher.find()) {
			matches.add(Jsoup.parse(matcher.group()).text());
			System.out.println(matcher.group());
		}

		admission = matches.get(0);

		// String admission = text.select("div#eventText font").first().text();

		details = details.replace(date, "").replace(admission, "")
				.replace(", Einschränkung", "Einschränkung")
				.split("Mehr Infos im Internet:")[0].split("von:")[0].trim();

		if(date.matches(".*\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d.*")) {
			String[] dateLocation = date.split(",", 2);

			date = dateLocation[0];
			location = dateLocation[1].replace(", zum Stadtplan", "").trim();
		}
		else {
			location = date;
			date = "Dauerbrenner";
		}

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
				calIntent.putExtra(Events.DESCRIPTION, details + "\n\n" + link);
				System.out.println(date);
				String[] dates = date.split("bis");
				String fromDate = dates[0].trim();
				System.out.println(fromDate);
				String[] fromDateParts = fromDate.split("\\.");
				GregorianCalendar calDate = new GregorianCalendar(
						Integer.parseInt(fromDateParts[2]),
						Integer.parseInt(fromDateParts[1]) - 1,
						Integer.parseInt(fromDateParts[0]));
				calIntent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);
				calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
						calDate.getTimeInMillis());
				if(dates.length > 1)
				{
					String toDate = dates[1].trim();
					System.out.println(toDate);
					String[] toDateParts = toDate.split("\\.");
					calDate = new GregorianCalendar(
							Integer.parseInt(toDateParts[2]),
							Integer.parseInt(toDateParts[1]) - 1,
							Integer.parseInt(toDateParts[0]) + 1);
				}
				calIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
						calDate.getTimeInMillis());
				startActivity(calIntent);
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
