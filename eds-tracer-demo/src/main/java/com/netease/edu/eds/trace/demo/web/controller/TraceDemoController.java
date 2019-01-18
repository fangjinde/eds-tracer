package com.netease.edu.eds.trace.demo.web.controller;/**
                                                       * Created by hzfjd on 18/4/13.
                                                       */

import com.google.common.collect.Lists;
import com.netease.edu.eds.trace.demo.constants.TransactionMessageTestContants;
import com.netease.edu.eds.trace.demo.domain.ndir.StudyCourseDocument;
import com.netease.edu.eds.trace.demo.dto.DemoDocumentDto;
import com.netease.edu.eds.trace.demo.dto.DemoDto;
import com.netease.edu.eds.trace.demo.service.AsyncTestService;
import com.netease.edu.eds.trace.demo.service.EduAttributesService;
import com.netease.edu.eds.trace.demo.service.StudyCourseSearchDao;
import com.netease.edu.eds.trace.demo.service.TransactionMessageTestService;
import com.netease.edu.eds.trace.utils.EnvironmentUtils;
import com.netease.edu.job.share.JobShareService;
import com.netease.edu.job.share.dto.ResultDTO;
import com.netease.edu.job.share.dto.TaskIdDTO;
import com.netease.edu.persist.redis.JedisClient;
import com.netease.edu.util.collection.BaseQuery;
import com.netease.edu.util.collection.PaginationResult;
import com.netease.edu.web.viewer.ResponseView;
import com.netease.ndir.client.NDirIndexClient;
import com.netease.ndir.client.config.SearchConfig;
import com.netease.ndir.client.config.SortField;
import com.netease.ndir.common.exception.NDirException;
import com.netease.ndir.common.index.IndexOperation;
import com.netease.ndir.common.index.VerIndexRequest;
import com.netease.ndir.common.schema.FieldType;
import com.netease.ndir.common.syntax.ClauseQuerySyntax;
import com.netease.ndir.common.syntax.NDirQuery;
import com.netease.ndir.common.syntax.PhraseQuerySyntax;
import com.netease.ndir.common.syntax.QueryOccur;
import net.spy.memcached.MemcachedClient;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.DefaultResultMapper;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ResultsMapper;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

/**
 * @author hzfjd
 * @create 18/4/13
 */

@RestController
public class TraceDemoController {

    @Autowired
    private RestTemplate                  restTemplateDemo;

    @Resource(name = "traceDemoAmqpTemplate")
    private AmqpTemplate                  traceDemoAmqpTemplate;

    @Autowired
    private StudyCourseSearchDao          studyCourseSearchDao;
    @Autowired
    private NDirIndexClient               ndirIndexClient;

    @Resource(name = "studyRedisTemplate")
    private RedisTemplate<String, Object> studyRedisTemplate;

    @Resource(name = "studyCounterRedisTemplate")
    private RedisTemplate<String, String> studyCounterRedisTemplate;

    @Resource(name = "studyJedisClient")
    private JedisClient                   studyJedisClient;

    @Resource(name = "studyMemcachedClient")
    private MemcachedClient               studyMemcachedClient;

    @Resource(name = "bareMemcachedClient")
    private MemcachedClient               bareMemcachedClient;

    @Autowired
    private EduAttributesService          eduAttributesService;

    private ExecutorService               executorService = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS,
                                                                                   new LinkedBlockingQueue<Runnable>());

    @Autowired
    private ElasticsearchOperations       elasticsearchOperations;
    @Autowired
    private AsyncTestService              asyncTestService;

    @Autowired
    private TransactionMessageTestService transactionMessageTestService;
    @Autowired
    private JobShareService               jobShareService;

    @Value("${spring.application.name}.${spring.profiles.active}")
    private String                        serviceId;

    @RequestMapping(path = "/webClient/sendByRestTemplate")
    public ResponseView sendByRestTemplate() {
        ResponseView responseView = new ResponseView();
        Map<String, Object> map = new HashMap<>();

        String pong = restTemplateDemo.getForObject(String.format("http://%s/web/echo?ping={1}", serviceId), String.class,
                                                    "hello");

        map.put("pong", pong);
        responseView.setResult(map);
        return responseView;
    }

    @RequestMapping(path = "/web/echo")
    public String webEcho(@RequestParam(required = false, name = "ping") String ping) {
        return ping;
    }

    @RequestMapping(path = "/job/submitDelayTask")
    public ResponseView submitDelayTask(@RequestParam("bizId") String bizId,
                                        @RequestParam(value = "delaySeconds", required = false) Long delaySeconds) {
        ResponseView responseView = new ResponseView();
        Map<String, Object> map = new HashMap<>();

        TaskIdDTO taskIdDTO = new TaskIdDTO();
        taskIdDTO.setEnvironment(EnvironmentUtils.getCurrentEnv());
        taskIdDTO.setBizId(bizId);
        taskIdDTO.setBizType("deploy_job_shuffle_demo");

        if (delaySeconds == null) {
            delaySeconds = 120L;
        }
        ResultDTO resultDTO = jobShareService.submitDelayTask(taskIdDTO, null,
                                                              System.currentTimeMillis() + delaySeconds * 1000);

        responseView.setResult(resultDTO);
        return responseView;
    }

    @RequestMapping(path = "/async/do")
    public ResponseView asyncDo() {
        ResponseView responseView = new ResponseView();
        Map<String, Object> map = new HashMap<>();
        asyncTestService.asyncDo();
        responseView.setResult(map);
        return responseView;
    }

    @RequestMapping(path = "/mvc/requestBody", method = RequestMethod.POST)
    public ResponseView requestBody(@RequestBody DemoDto demoDto, @RequestParam("paramId") String paramId) {
        ResponseView responseView = new ResponseView();
        Map<String, Object> map = new HashMap<>();
        map.put("demoDto", demoDto);
        map.put("paramId", paramId);
        responseView.setResult(map);
        return responseView;
    }

    @RequestMapping(path = "/ddb/ops")
    public ResponseView ddbOps() {
        String key = "ddb_trace_demo_ops_key";
        boolean updated = eduAttributesService.updateAttributes(key + String.valueOf(new Random().nextLong()),
                                                                String.valueOf(new Random().nextLong()));
        String value = eduAttributesService.getAttributesValue(key);
        ResponseView responseView = new ResponseView();
        responseView.setResult(value);
        return responseView;

    }

    @RequestMapping(path = "/setValue")
    public ResponseView setValue(@RequestParam(value = "key", required = false) String key,
                                 @RequestParam(value = "value", required = false) String value) {

        boolean updated = eduAttributesService.updateAttributes(key, value);

        ResponseView responseView = new ResponseView();
        responseView.setResult(updated);
        return responseView;

    }

    /**
     * corresponding message consumer see TraceDemoMessageListener.class
     *
     * @param msg
     * @return
     */
    @RequestMapping(path = "/rabbit/send")
    public ResponseView sendMessage(@RequestParam(required = false, name = "msg") String msg) {

        if (msg == null || msg.length() == 0) {
            msg = "default";
        }
        traceDemoAmqpTemplate.convertAndSend(msg);

        ResponseView responseView = new ResponseView();
        responseView.setResult("ok");
        return responseView;
    }

    @RequestMapping(path = "/ndir/search")
    public ResponseView searchCourse(@RequestParam(required = false, name = "keyword") String keyword) {

        SearchConfig searchConfig = new SearchConfig();
        List<SortField> sortFields = Lists.newArrayList();
        sortFields.add(new SortField("uniform_combo_score", true));
        sortFields.add(new SortField("publish_time", true));
        searchConfig.setSortFields(sortFields.toArray(new SortField[sortFields.size()]));

        List<ClauseQuerySyntax> clauses = new ArrayList<ClauseQuerySyntax>();
        clauses.add(new ClauseQuerySyntax(new NDirQuery(new PhraseQuerySyntax("course_name_suff_sepa", keyword, false,
                                                                              100f)),
                                          QueryOccur.SHOULD, 100f));
        clauses.add(new ClauseQuerySyntax(new NDirQuery(new PhraseQuerySyntax("provider_suff_sepa", keyword, false,
                                                                              1f)),
                                          QueryOccur.SHOULD, 100f));

        int pageSize = 10;
        int pageIndex = 1;

        PaginationResult<StudyCourseDocument> result = studyCourseSearchDao.getByQueryCondition(new NDirQuery(clauses),
                                                                                                new BaseQuery(pageSize,
                                                                                                              pageIndex),
                                                                                                searchConfig);

        ResponseView responseView = new ResponseView();
        responseView.setResult(result.getList());
        return responseView;
    }

    @RequestMapping(path = "/ndir/index")
    public ResponseView indexAggregationLearn() throws NDirException {

        List<VerIndexRequest> indexDocRequestList4Delete = Lists.newArrayList();
        addRecords(indexDocRequestList4Delete, false, 20180425L, 20180426L, 20180427L);
        ndirIndexClient.indexAll("agregation_content", indexDocRequestList4Delete);

        List<VerIndexRequest> indexDocRequestList4Add = Lists.newArrayList();
        addRecords(indexDocRequestList4Add, true, 20180425L, 20180426L, 20180427L);
        ndirIndexClient.indexAll("agregation_content", indexDocRequestList4Add);

        ResponseView responseView = new ResponseView();
        responseView.setResult("index ok");
        return responseView;
    }

    @RequestMapping(path = "/redis/template/ops")
    public ResponseView redisTemplateValueSet() {

        String key1 = "trace_demo_redis_key1";
        String key2 = "trace_demo_redis_key2";
        studyRedisTemplate.opsForValue().set(key1, new DemoDto().withId(1L).withName("name"));
        DemoDto demoDto1 = (DemoDto) studyRedisTemplate.opsForValue().get(key1);
        studyRedisTemplate.opsForValue().set(key2, new DemoDto().withId(2L).withName("name2"));
        List<Object> lists1 = studyRedisTemplate.opsForValue().multiGet(Arrays.asList(key1, key2));

        String key3 = "trace_demo_redis_key3";
        studyCounterRedisTemplate.opsForValue().increment(key3, 2);
        String count = studyCounterRedisTemplate.opsForValue().get(key3);

        Map<String, Object> map = new HashMap<>();
        map.put("demoDto1", demoDto1);
        map.put("list1", lists1);
        map.put("count", count);
        ResponseView responseView = new ResponseView();
        responseView.setResult(map);
        return responseView;
    }

    @RequestMapping(path = "/redis/studyJedisClient/ops")
    public ResponseView studyJedisClientOps() {
        String key1 = "trace_demo_redis_list_key1";
        studyJedisClient.addList(key1, "1", "11", "111");
        List<String> vList = studyJedisClient.getList(key1);
        long count = studyJedisClient.countList(key1);
        Map<String, Object> map = new HashMap<>();
        map.put("list1", vList);
        map.put("count", count);
        ResponseView responseView = new ResponseView();
        responseView.setResult(map);
        return responseView;
    }

    @RequestMapping(path = "/memcache/studyMemcacheClient/ops")
    public ResponseView studyMemcachedClientOps() {
        String key1 = "trace_demo_memcache_key1";
        Long newInc = studyMemcachedClient.incr(key1, 3L, 0L, 600);
        String getValue = (String) studyMemcachedClient.get(key1);

        // use bareMemcachedClient because keyPrefixMemcachedClient impl error(connection shutdown before call) for
        // method getBulk.
        bareMemcachedClient.set("1", 600, "v1");
        bareMemcachedClient.set("2", 600, "v2");
        bareMemcachedClient.set("4", 600, "v4");
        bareMemcachedClient.set("5", 600, "v5");
        bareMemcachedClient.set("6", 600, "v6");
        Map<String, Object> multikeysRet = bareMemcachedClient.getBulk("1", "2", "3", "4", "5", "6");

        Map<String, Object> map = new HashMap<>();
        map.put("getValue", getValue);
        map.put("newInc", newInc);
        map.put("countOfBulk", multikeysRet.size());
        map.put("valueList", multikeysRet.values());
        ResponseView responseView = new ResponseView();
        responseView.setResult(map);
        return responseView;
    }

    @RequestMapping(path = "/async/threadOrWithPool/ops")
    public ResponseView asyncCalls() {

        asyncTestService.asyncDo();

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                runSth();
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();

        new CustomThread().start();

        executorService.submit(runnable);

        Map<String, Object> map = new HashMap<>();
        map.put("async", "ok");
        ResponseView responseView = new ResponseView();
        responseView.setResult(map);
        return responseView;

    }

    private void runSth() {
        String key = "ddb_trace_demo_ops_key";
        boolean updated = eduAttributesService.updateAttributes(key, String.valueOf(new Random().nextLong()));
    }

    class CustomThread extends Thread {

        @Override
        public void run() {
            runSth();
        }
    }

    @RequestMapping(path = "/es/ops")
    public ResponseView elasticSearchOps() {

        // celasticsearchOperations.

        updateOrAdd();

        String indexRet = singleIndex();

        bulkIndex();

        List<DemoDocumentDto> retList = queryByName();

        addOrUpdate();

        Map<String, Object> map = new HashMap<>();
        map.put("indexRet", indexRet);
        map.put("retList", retList);
        ResponseView responseView = new ResponseView();
        responseView.setResult(map);
        return responseView;
    }

    @RequestMapping(path = "/transactionMessage/ops")
    public ResponseView transactionMessageOps() {

        transactionMessageTestService.interProcessTransactionalBiz();

        String curTime = eduAttributesService.getAttributesValue(TransactionMessageTestContants.Key1);

        Map<String, Object> map = new HashMap<>();
        map.put("curTime", curTime);
        map.put("key1", TransactionMessageTestContants.Key1);
        map.put("key2", TransactionMessageTestContants.Key2);
        ResponseView responseView = new ResponseView();
        responseView.setResult(map);
        return responseView;
    }

    @RequestMapping(path = "/transactionMessage/result")
    public ResponseView transactionMessageResult() {

        String v1 = eduAttributesService.getAttributesValue(TransactionMessageTestContants.Key1);
        String v2 = eduAttributesService.getAttributesValue(TransactionMessageTestContants.Key2);

        Map<String, Object> map = new HashMap<>();
        map.put("v1", v1);
        map.put("v2", v2);
        ResponseView responseView = new ResponseView();
        responseView.setResult(map);
        return responseView;
    }

    private void addThenDelete() {
        String id = "20180605-addThenDelete";
        GetQuery getQuery = new GetQuery();
        getQuery.setId(id);

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setObject(new DemoDocumentDto().withId(id).withName("fixed index" + System.currentTimeMillis()));
        elasticsearchOperations.index(indexQuery);

        DemoDocumentDto demoDocumentDto = elasticsearchOperations.queryForObject(getQuery, DemoDocumentDto.class);

        elasticsearchOperations.delete(DemoDocumentDto.class, id);

        DemoDocumentDto demoDocumentDto2 = elasticsearchOperations.queryForObject(getQuery, DemoDocumentDto.class);

    }

    private void updateOrAdd() {

        String id = null;

        if (new Random().nextInt() % 2 == 0) {
            id = "20180605-updateOrAdd-exist";
        } else {
            id = "20180605-updateOrAdd-" + new Random().nextInt();
        }

        ResultsMapper resultsMapper = new DefaultResultMapper();
        IndexRequest indexRequest = new IndexRequest();
        try {
            indexRequest.source(resultsMapper.getEntityMapper().mapToString(new DemoDocumentDto().withId(id).withName("update or add case"
                                                                                                                      + System.currentTimeMillis())));
        } catch (IOException e) {
            e.printStackTrace();
        }

        UpdateRequest uRequest = new UpdateRequest();
        uRequest.doc(indexRequest);

        UpdateQuery updateQuery = new UpdateQuery();
        updateQuery.setId(id);
        updateQuery.setDoUpsert(true);
        updateQuery.setClazz(DemoDocumentDto.class);
        updateQuery.setUpdateRequest(uRequest);
        elasticsearchOperations.update(updateQuery);
    }

    private void addOrUpdate() {
        GetQuery getQuery = new GetQuery();
        String id = "20180605";
        getQuery.setId(id);

        DemoDocumentDto demoDocumentDto = elasticsearchOperations.queryForObject(getQuery, DemoDocumentDto.class);

        if (demoDocumentDto == null) {
            IndexQuery indexQuery = new IndexQuery();
            indexQuery.setObject(new DemoDocumentDto().withId(id).withName("fixed index" + System.currentTimeMillis()));
            elasticsearchOperations.index(indexQuery);

        } else {

            UpdateRequest uRequest = new UpdateRequest();
            try {
                // uRequest.doc(jsonBuilder().startObject().field("name", "学习目标 掌握java泛型的产生意义ssss").endObject());
                uRequest.doc("name", "就是这样更新单个字段");
            } catch (Exception e) {
                e.printStackTrace();
            }

            UpdateQuery updateQuery = new UpdateQuery();
            updateQuery.setId(id);
            updateQuery.setClazz(DemoDocumentDto.class);
            updateQuery.setUpdateRequest(uRequest);
            elasticsearchOperations.update(updateQuery);

        }

    }

    private String singleIndex() {
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setObject(new DemoDocumentDto().withId(String.valueOf(new Random().nextLong())).withName("single index"));
        String indexRet = elasticsearchOperations.index(indexQuery);
        return indexRet;
    }

    private void bulkIndex() {
        List<IndexQuery> indexQueryList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            IndexQuery indexQuery = new IndexQuery();
            indexQuery.setObject(new DemoDocumentDto().withId(String.valueOf(new Random().nextLong())).withName("bulk index -"
                                                                                                                + i));
            indexQueryList.add(indexQuery);
        }

        elasticsearchOperations.bulkIndex(indexQueryList);
    }

    private List<DemoDocumentDto> queryByName() {

        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(matchQuery("name",
                                                                                      "index")).withPageable(PageRequest.of(0,
                                                                                                                            10)).build();

        List<DemoDocumentDto> retList = elasticsearchOperations.queryForList(searchQuery, DemoDocumentDto.class);
        return retList;
    }

    private void addRecords(List<VerIndexRequest> indexDocRequestList, boolean addOrDelete, Long... ids) {
        for (Long id : ids) {
            VerIndexRequest indexDocRequest = new VerIndexRequest();
            indexDocRequest.setDataValue("id", id + "-" + 111111, FieldType.STRING);
            indexDocRequest.setDataValue("product_id", 1L, FieldType.LONG);
            indexDocRequest.setDataValue("title", "title", FieldType.STRING);
            indexDocRequest.setDataValue("desc", "desc", FieldType.STRING);
            indexDocRequest.setDataValue("type", 111111, FieldType.INT);
            indexDocRequest.setDataValue("keywords", "keywords", FieldType.STRING);
            indexDocRequest.setDataValue("recommend_tag", "recommend_tag", FieldType.STRING);
            if (addOrDelete) {
                indexDocRequest.setOp(IndexOperation.ADD);
            } else {
                indexDocRequest.setOp(IndexOperation.DELETE);
            }

            indexDocRequestList.add(indexDocRequest);
        }

    }

}
