# rest-client
Java Proxy client for REST services.

Here is a functional example for Wikipedia API. First need to create DTO (Data Transport Object) compatible with returned JSON from API.
Next define Java interface for Wikipedia API; need to define only methods of interest not all existing on API interface.

Create REST client factory and use it to create Wikipedia client instance. Need to provide Wikipedia API base URL. Once client instance
created use it as regular Java instance and call methods. Client instance is a Java Proxy that convert method invocation into REST call.

```
  public class WikipediaPageSummary {
    String title;
    String displayTitle;
    String extract;
  }

  public interface Wikipedia {
    @Path("page/summary/{title}")
    WikipediaPageSummary getPageSummary(@PathParam("title") String title);
  }

  RestClientFactory factory = new RestClientFactory();
  Wikipedia wikipedia = factory.newInstance("https://en.wikipedia.org/api/rest_v1/", Wikipedia.class);
  
  WikipediaPageSummary summary = wikipedia.getPageSummary("Lion");
  // summary instance with fields initialized from JSON response
```  
  
