import io.vertx.core.http.HttpMethod
import io.vertx.groovy.core.http.HttpClient
import io.vertx.groovy.core.http.HttpServer
import io.vertx.rx.groovy.RxHelper
import io.vertx.rx.groovy.test.MyPojo

HttpServer server = vertx.createHttpServer(port:8080);
server.requestStream().handler({ req ->
  req.response().setChunked(true).end("{\"foo\":\"bar\"}");
});
server.listen({ ar ->
  HttpClient client = vertx.createHttpClient();
  def req = client.request(HttpMethod.GET, 8080, "localhost", "/the_uri");
  def observable = req.toObservable();
  def objects = [];
  observable.
    flatMap({resp -> resp.toObservable()}).
    lift(RxHelper.unmarshaller(MyPojo.class)).
    forEach(
      objects.&add,
      {err -> test.fail(err.message)}, { ->
    server.close();
    test.assertEquals(Arrays.asList(new MyPojo("bar")), objects);
    test.testComplete();
  });
  req.end();
});
test.await();
