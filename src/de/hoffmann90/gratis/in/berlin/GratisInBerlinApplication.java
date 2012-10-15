package de.hoffmann90.gratis.in.berlin;

import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formKey = "dGkzTWthR1JjMVdFVVh2VXZpbUVLelE6MQ")
public class GratisInBerlinApplication extends Application {

	@Override
	public void onCreate() {
		// The following line triggers the initialization of ACRA
		ACRA.init(this);
		super.onCreate();
	}
}