package com.maynooth.web.scrapper.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
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
	String hiddenCommentLink = "";
	List<Comment> pendingComments = new ArrayList<>();
	LinkedHashSet<Comment> comments = new LinkedHashSet<>();

	@Override
	public void readData() throws IOException, ParseException {
		
		Document doc = Jsoup.connect("https://entertainment.slashdot.org/story/21/06/09/0739253/is-hbo-max-broken").get();
		//Document doc = Jsoup.connect("https://it.slashdot.org/story/21/06/16/2258231/the-global-chip-shortage-is-creating-a-new-problem-more-fake-components").get();
		writeToFile(doc.toString(),"srap_comment_temp1");
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
		
		Element dept = storyPostedBy.getElementsByClass("dept-text").first();
		System.out.println(dept.text());
		articleDb.setFromDept(dept.text());
		dept.remove();
		
		Element time = storyPostedBy.select("time").get(0);
		Date date = parseDate(time.text());
		articleDb.setPostTime(date);
		time.remove();
		
		System.out.println(storyPostedBy.ownText());
		String by= storyPostedBy.ownText().replace("Posted by", "").replace("from the", "")
				.replace("dept.", "").trim();
		System.out.println(by);
		articleDb.setPostedBy(by);
		
		System.out.println(article.getElementsByClass("body").text());
		articleDb.setContent(article.getElementsByClass("body").text());

		articleDb = articleRepository.saveAndFlush(articleDb);
		return articleDb;
	}

	private void readComments(Document doc, Article article) throws IOException, ParseException
	{
		Element commentList = doc.getElementById("commentlisting");
		Elements parentComments = commentList.children();
		//writeToFile(comments.toString());
		for(Element c : parentComments)
			readComment(c, null, article);
		
		if(!comments.isEmpty())
			commentRepository.saveAll(comments);
			
	}
	
	private void readComment(Element c,Comment parrentCmmt, Article article) throws ParseException, IOException
	{
		CommentClass cmmtCls = CommentClass.getByDesc(c.select("li").get(0).className());
		if(null != cmmtCls)
		{
			int cmmtId = getCommentCode(c);

			Comment comment = new Comment();
			comment.setArticle(article);
			comment.setParentComment(parrentCmmt);
			comment.setCommentCode(cmmtId);
			comment.setCls(cmmtCls);

			if(cmmtCls.equals(CommentClass.FULL_CONTAIN))
			{
				System.out.println("<------------------------------- Reading Full Comment ------------------------------>");

				readCommentDetails(c,comment);
				Elements replies = getReplyCount(c, comment);
				readCommentReplies(replies,comment,article);
				comments.add(comment);
			}
			else if(cmmtCls.equals(CommentClass.ONELINE))
			{
				System.out.println("<------------------------------- Reading Oneline Comment ------------------------------>");

				commentLink = "https:"+getCommentLink(c,cmmtId);
				c = scrapCommentSection(commentLink, cmmtId);
				
				readCommentDetails(c,comment);
				Elements replies = getReplyCount(c, comment);
				readCommentReplies(replies,comment,article);
				comments.add(comment);
			}
			else if(cmmtCls.equals(CommentClass.HIDDEN))
			{
				System.out.println("<------------------------------- Reading Hidden Comment ------------------------------>");

				if(commentLink.isEmpty())
					pendingComments.add(comment);
				else
				{
					if(hiddenCommentLink.isEmpty())
					{
						String linkParams[] = commentLink.split("cid=");
						hiddenCommentLink = linkParams[0]+"cid=";
					}
					String link = hiddenCommentLink + cmmtId; 
					c = scrapCommentSection(link, cmmtId);
					
					readCommentDetails(c,comment);
					Elements replies = getReplyCount(c, comment);
					readCommentReplies(replies,comment,article);
					comments.add(comment);
				}
			}
		}
	}
	
	private void readCommentDetails(Element c,Comment commentDb) throws ParseException
	{
		Element comment = c.getElementById("comment_"+commentDb.getCommentCode());
		readCommentTitle(comment, commentDb);
		readCommentByAndTime(comment,commentDb);
		readCommentBody(comment,commentDb);
	}
	
	private void readCommentTitle(Element comment, Comment commentDb)
	{
		
		Elements temp = comment.getElementsByClass("title");
		if(null != temp)
		{
			Element cmmtTitle = temp.get(0);
			String title = cmmtTitle.getElementsByAttributeValue("id", "comment_link_"
					+commentDb.getCommentCode()).get(0).text();
			System.out.println("title: "+title);
			commentDb.setTitle(title);
			
			String type = cmmtTitle.getElementsByAttributeValue("id", "comment_score_"
					+commentDb.getCommentCode()).get(0).text();
			
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
	
	private void readCommentByAndTime(Element comment, Comment commentDb) throws ParseException
	{
		Elements temp = comment.getElementsByClass("details");
		if(null != temp)
		{
			Element cmmtBy = temp.get(0);
			String by = cmmtBy.getElementsByClass("by").get(0).text().replace("by", "").trim();
			System.out.println("by: "+by);
			commentDb.setPostedBy(by);
			
			String time = cmmtBy.getElementsByClass("otherdetails").get(0).text();
			commentDb.setPostTime(parseDate(time));
		}
	}
	
	private void readCommentBody(Element comment, Comment commentDb)
	{
		String body = comment.getElementsByAttributeValue("id", "comment_body_"
				+commentDb.getCommentCode()).get(0).text();
		System.out.println("body: "+body);
		commentDb.setContent(body);
	}
	
	private Document scrapCommentSection(String link, int id) throws IOException
	{
		Document doc = Jsoup.connect(link).get();
		writeToFile(doc.toString(),"tree_"+id);
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
	
	private void readCommentReplies(Elements replies, Comment comment, Article article) throws ParseException, IOException 
	{	
		System.out.println("<------------------------------- Reading Replies ------------------------------>");

		if(null != replies)
		{
			for(Element r:replies)
				readComment(r, comment, article);
		}
	}

	private Elements getReplyCount(Element c, Comment comment)
	{
		Elements replies = null;
		Element cmmtRlyTree = c.getElementsByAttributeValueMatching("id", "commtree_"+comment.getCommentCode())
				.first();
		if(null != cmmtRlyTree)
		{	
			replies = cmmtRlyTree.children();
			if(null != replies)
				comment.setReplyCount(replies.size());
		}
		return replies;
	}

	private int getCommentCode(Element comment)
	{
		String cmmtId = comment.getElementsByAttributeValueMatching("id", "tree_\\d{8}").first()
				.attr("id").replace("tree_", "");
		System.out.println("id: "+cmmtId);
		return Integer.parseInt(cmmtId);
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
