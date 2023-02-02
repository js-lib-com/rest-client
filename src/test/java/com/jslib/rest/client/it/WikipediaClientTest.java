package com.jslib.rest.client.it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jslib.rest.client.RestClientFactory;

public class WikipediaClientTest {
	private static RestClientFactory factory;

	@BeforeClass
	public static void beforeClass() {
		factory = new RestClientFactory();
	}

	private Wikipedia wikipedia;

	@Before
	public void beforeTest() {
		wikipedia = factory.getRemoteInstance("https://en.wikipedia.org/api/rest_v1/", Wikipedia.class);
	}

	@Test
	public void getPageSummary() {
		WikipediaPageSummary summary = wikipedia.getPageSummary("Lion");
		assertThat(summary, notNullValue());
		assertThat(summary.getTitle(), notNullValue());
		assertThat(summary.getTitle(), equalTo("Lion"));
		assertThat(summary.getDisplayTitle(), notNullValue());
		assertThat(summary.getDisplayTitle(), equalTo("<span class=\"mw-page-title-main\">Lion</span>"));
		assertThat(summary.getExtract(), notNullValue());
		assertThat(summary.getExtract(), startsWith("The lion is a large cat of the genus Panthera"));
	}

	public static class WikipediaPageSummary {
		private String title;
		private String displaytitle;
		private String extract;

		public String getTitle() {
			return title;
		}

		public String getDisplayTitle() {
			return displaytitle;
		}

		public String getExtract() {
			return extract;
		}
	}

	public static interface Wikipedia {
		@Path("page/summary/{title}")
		WikipediaPageSummary getPageSummary(@PathParam("title") String title);
	}
}
