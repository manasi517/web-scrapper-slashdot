package com.maynooth.web.scrapper.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.maynooth.web.scrapper.entity.Article;
import com.maynooth.web.scrapper.entity.Comment;
import com.maynooth.web.scrapper.enums.CommentClass;
import com.maynooth.web.scrapper.repository.ArticleRepository;
import com.maynooth.web.scrapper.repository.CommentRepository;

@Service
public class WebScrappingServiceImpl implements WebCrappingService {

	@Autowired
	ArticleRepository articleRepository;
	
	@Autowired
	CommentRepository commentRepository;
	
	String commentLink = "";

	@Override
	public void readData() throws IOException, ParseException {

		/*
		 * WebClient client = new WebClient(); client.getOptions().setCssEnabled(false);
		 * client.getOptions().setJavaScriptEnabled(false); String searchUrl =
		 * "https://entertainment.slashdot.org/story/21/06/09/0739253/is-hbo-max-broken";
		 * HtmlPage page = client.getPage(searchUrl); List<DomElement> items =
		 * page.getElementsById("commentlisting"); for(DomNode item :
		 * items.get(0).getChildren()){ System.out.println(item.toString()); }
		 */
		
		
		Document doc = Jsoup.connect("https://entertainment.slashdot.org/story/21/06/09/0739253/is-hbo-max-broken").get();
		//writeToFile(doc.toString(),"srap_comment_temp");
		Article article = readArticle(doc);
		readComments(doc,article);

	}

	private Article readArticle(Document doc) throws IOException, ParseException
	{
		System.out.println("<------------------------------- Reading Article ------------------------------>");
		Element article = doc.select("article").get(0);

		Article articleDb = new Article(); 
		Element articleHead = article.select("header").get(0); 
		Element storyTitle = articleHead.getElementsByAttributeValue("class", "story-title").get(0);
		Element storySource = articleHead.getElementsByAttributeValue("class","story-sourcelnk").get(0); 
		Element storyPostedBy = articleHead.getElementsByAttributeValue("class", "story-byline").get(0);
		Elements comments = articleHead.getElementsByClass("comment-bubble");
		Element commentCnt = null; if(null != comments && !comments.isEmpty()) 
		{ 
			commentCnt = comments.get(0); 
			System.out.println(commentCnt.text());
			articleDb.setCommentCount(Integer.parseInt(commentCnt.text()));
		}

		System.out.println(storyTitle.children().get(0).text());
		articleDb.setTitle(storyTitle.children().get(0).text());
		
		System.out.println(storySource.attr("href"));
		articleDb.setSource(storySource.attr("href"));
		
		System.out.println(storyPostedBy.select("a").get(0).text());
		articleDb.setPostedBy(storyPostedBy.select("a").get(0).text());
		
		//System.out.println(storyPostedBy.select("time").get(0).text());
		String time = storyPostedBy.select("time").get(0).text();
		Date date = parseDate(time);
		articleDb.setPostTime(date);
		
		System.out.println(storyPostedBy.getElementsByClass("dept-text").text());
		articleDb.setFromDept(storyPostedBy.getElementsByClass("dept-text").text());
		
		System.out.println(article.getElementsByClass("body").text());
		articleDb.setContent(article.getElementsByClass("body").text());

		articleDb = articleRepository.saveAndFlush(articleDb);
		return articleDb;
	}

	private void readComments(Document doc, Article article) throws IOException, ParseException
	{
		Element commentList = doc.getElementById("commentlisting");
		Elements comments = commentList.children();
		//writeToFile(comments.toString());
		for(Element c : comments)
			readComment(c, null, article);
	}
	private void readComment(Element c,Comment parrentCmmt, Article article) throws ParseException, IOException
	{
		CommentClass cmmtCls = CommentClass.getByDesc(c.select("li").get(0).className());
		
		Comment comment = new Comment();
		comment.setArticle(article);
		comment.setParentComment(parrentCmmt);
		
		if(null != cmmtCls && cmmtCls.equals(CommentClass.FULL_CONTAIN))
		{
			System.out.println("<------------------------------- Reading Full Comment ------------------------------>");
			
			int cmmtId = getCommentCode(c);
			comment.setCommentCode(cmmtId);
			
			readCommentTitle(c,cmmtId,comment);
			readCommentBy(c,comment);
			readCommentBody(c,cmmtId,comment);
			readCommentReplies(c,comment,article);
		}
		else if(null != cmmtCls && cmmtCls.equals(CommentClass.ONELINE))
		{
			System.out.println("<------------------------------- Reading Oneline Comment ------------------------------>");
			
			int cmmtId = getCommentCode(c);
			comment.setCommentCode(cmmtId);
			
			//readCommentDetails(c,comment);
			commentLink = "https:"+getCommentLink(c,cmmtId);
			
			Element doc = scrapCommentSection(cmmtId);
			if(null != doc)
			{
				
			}
			//readCommentReplies(c,comment,article);
		}
	}
	
	private Document scrapCommentSection(int id) throws IOException
	{
		Document doc = Jsoup.connect(commentLink).get();
		writeToFile(doc.toString(),"comment_"+id);
		return doc;
	}
	
	private String getCommentLink(Element c, int id)
	{
		String cmmtLink = null;
		Element link = c.getElementsByAttributeValue("id", "comment_link_"+id).get(0);
		if(null != link)
			cmmtLink = link.attr("href"); 
		
		return cmmtLink;
	}
	
	private void readCommentReplies(Element c, Comment comment, Article article) throws ParseException, IOException 
	{	
		System.out.println("<------------------------------- Reading Replies ------------------------------>");
		
		Element cmmtRlyTree = c.getElementsByAttributeValueMatching("id", "commtree_"+comment.getCommentCode())
				.first();
		if(null != cmmtRlyTree)
		{
			Elements replies = cmmtRlyTree.children();
			if(null != replies)
			{
				comment.setReplyCount(replies.size());
				for(Element r:replies)
					readComment(r, comment, article);
			}
		}
	}


	private int getCommentCode(Element comment)
	{
		String cmmtId = comment.getElementsByAttributeValueMatching("id", "comment_\\d{8}").first()
				.attr("id").replace("comment_", "");
		System.out.println("id: "+cmmtId);
		return Integer.parseInt(cmmtId);
	}
	
	private void readCommentTitle(Element comment,int id, Comment commentDb)
	{
		Elements temp = comment.getElementsByClass("title");
		if(null != temp)
		{
			Element cmmtTitle = temp.get(0);
			String title = cmmtTitle.getElementsByAttributeValue("id", "comment_link_"+id).get(0).text();
			System.out.println("title: "+title);
			commentDb.setTitle(title);
			
			/*
			 * String score = cmmtTitle.getElementsByAttributeValue("href",
			 * "#").get(0).text().replace("Score:", "");
			 * System.out.println("score: "+score);
			 * commentDb.setScore(Integer.parseInt(score));
			 */
			
			String type = cmmtTitle.getElementsByAttributeValue("id", "comment_score_"+id).get(0).text();
			System.out.println("type: "+type);
			String[] scores = type.replace("(", "").replace(")", "").trim().split(",");
			System.out.println("score: "+scores[0].replace("Score:", ""));
			commentDb.setScore(Integer.parseInt(scores[0].replace("Score:", "")));
			
			if(scores.length>1)
			{
				commentDb.setType(scores[1].trim());
				System.out.println("type: "+scores[1].trim());
			}
			
		}
	}
	
	private void readCommentBy(Element comment, Comment commentDb) throws ParseException
	{
		Elements temp = comment.getElementsByClass("details");
		if(null != temp)
		{
			Element cmmtBy = temp.get(0);
			String by = cmmtBy.getElementsByClass("by").select("a").get(0).text();
			System.out.println("by: "+by);
			commentDb.setPostedBy(by);
			
			String time = cmmtBy.getElementsByClass("otherdetails").get(0).text();
			commentDb.setPostTime(parseDate(time));
		}
	}
	
	private void readCommentBody(Element comment,int cmmtId, Comment commentDb)
	{
		String body = comment.getElementsByAttributeValue("id", "comment_body_"+cmmtId).get(0).text();
		System.out.println("body: "+body);
		commentDb.setContent(body);
	}
	
	private Date parseDate(String time) throws ParseException
	{	
		  Pattern pattern = Pattern.compile("on (.*?) [(].*"); 
		  Matcher matcher = pattern.matcher(time); 
		  if (matcher.find()) {
			  //System.out.println(matcher.group(1));
			  time = matcher.group(1);
		  }
		  else
		  {
			  Pattern pattern1 = Pattern.compile("on (.*?)M"); 
			  Matcher matcher1 = pattern1.matcher(time); 
			  if (matcher1.find()) {
				  //System.out.println(matcher1.group(1)); 
				  time = matcher1.group(1)+"M";
			  }
		  }
		 
		/*
		 * for(String s:time.split("[\s][o][n][\s].*[AP][M]")) System.out.println(s);
		 */
		//System.out.println();
		//System.out.println(DateUtils.parseDate(time, "EEEE MMMM dd, yyyy hh:mma"));
		DateFormat format = new SimpleDateFormat("EEEE MMMM dd, yyyy hh:mmaaa", Locale.ENGLISH);
		time = time.replaceFirst("@", "").trim();
		Date date = format.parse(time);
		System.out.println("date: "+date);
		return date;
	}
	
	private void writeToFile(String s, String fileName) throws IOException
	{
		
		 File yourFile = new File(fileName+".txt"); 
		 yourFile.createNewFile();
		 BufferedWriter writer = new BufferedWriter(new FileWriter(yourFile));
		 writer.write(s); 
		 writer.close();
		 
	}
}
