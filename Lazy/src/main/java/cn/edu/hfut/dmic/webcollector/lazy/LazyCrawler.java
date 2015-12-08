/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.edu.hfut.dmic.webcollector.lazy;

import cn.edu.hfut.dmic.webcollector.lazy.util.MongoHelper;
import cn.edu.hfut.dmic.webcollector.model.CrawlDatum;
import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.model.Page;
import cn.edu.hfut.dmic.webcollector.net.HttpRequest;
import cn.edu.hfut.dmic.webcollector.net.HttpResponse;
import cn.edu.hfut.dmic.webcollector.net.Proxys;
import cn.edu.hfut.dmic.webcollector.plugin.berkeley.BreadthCrawler;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hu
 */
public class LazyCrawler extends BreadthCrawler {
    
    public static final Logger LOG=LoggerFactory.getLogger(LazyCrawler.class);

    protected MongoHelper mongoHelper;
    protected HashMap<String, String> headerMap;
    protected Proxys proxys = new Proxys();

    public LazyCrawler(LazyConfig lazyConfig) {
        super(lazyConfig.getCrawlPath(), true);
        mongoHelper = new MongoHelper(lazyConfig.getMongoIP(), lazyConfig.getMongoPort(), lazyConfig.getPageDB(), lazyConfig.getPageCollection());
        for (String seed : lazyConfig.seeds) {
            this.addSeed(new CrawlDatum(seed).putMetaData("refer", "seed"));
        }
        for (String forcedSeed : lazyConfig.forcedSeeds) {
            this.addSeed(new CrawlDatum(forcedSeed).putMetaData("refer", "forced_seed"), true);
        }
        this.setRegexRule(lazyConfig.getRegexRule());
        this.setResumable(lazyConfig.isResumable());
        this.setTopN(lazyConfig.getTopN());
        this.headerMap = lazyConfig.getHeaderMap();
        this.proxys = lazyConfig.getProxys();
        this.setRetry(lazyConfig.getRetry());
        this.setVisitInterval(lazyConfig.getVisitInterval());
        this.setRetryInterval(lazyConfig.getRetryInterval());
        this.setThreads(lazyConfig.getThreads());
    }

    @Override
    public HttpResponse getResponse(CrawlDatum crawlDatum) throws Exception {
        HttpRequest request = new HttpRequest(crawlDatum);
        for (Entry<String, String> entry : headerMap.entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }
        if (proxys != null) {
            Proxy proxy = proxys.nextRandom();
            request.setProxy(proxy);
        }
        return request.getResponse();
    }

    @Override
    public void visit(Page page, CrawlDatums next) {
        String conteType = page.getResponse().getContentType();
        if (conteType != null && conteType.contains("text/html")) {
            String id = page.getCrawlDatum().getKey();
            String refer = page.getMetaData("refer");
            mongoHelper.addPage(id, page.getUrl(), refer, page.getHtml());
            
        }
    }

    @Override
    public void afterVisit(Page page, CrawlDatums next) {
        super.afterVisit(page, next);
        next.putMetaData("refer", page.getUrl());
    }

}
