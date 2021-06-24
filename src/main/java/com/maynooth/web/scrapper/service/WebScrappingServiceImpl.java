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

import javax.persistence.EntityNotFoundException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.maynooth.web.scrapper.dto.ArticleDto;
import com.maynooth.web.scrapper.dto.CommentDto;
import com.maynooth.web.scrapper.enums.CommentClass;
import com.maynooth.web.scrapper.mapper.ArticleMapper;
import com.maynooth.web.scrapper.mapper.CommentMapper;
import com.maynooth.web.scrapper.repository.ArticleRepository;
import com.maynooth.web.scrapper.repository.CommentRepository;

@Service
public class WebScrappingServiceImpl implements WebCrappingService {

	@Autowired
	ArticleRepository articleRepository;
	
	@Autowired
	CommentRepository commentRepository;
	
	@Autowired 
	ArticleMapper articleMapper;
	
	@Autowired 
	CommentMapper commentMapper;
	
	String commentLink = "";
	String hiddenCommentLink = "";
	LinkedHashSet<CommentDto> comments;
	List<ArticleDto> articles;

	@Override
	public void readData(int from, int to) throws IOException, ParseException {
		
		//Document doc = Jsoup.connect("https://entertainment.slashdot.org/story/21/06/09/0739253/is-hbo-max-broken").get();
		//Document doc = Jsoup.connect("https://it.slashdot.org/story/21/06/16/2258231/the-global-chip-shortage-is-creating-a-new-problem-more-fake-components").get();
		//(total are 5 but cant find )
		//Document doc = Jsoup.connect("https://tech.slashdot.org/story/21/06/20/2024255/googles-no-click-searches----good-or-evil").get();
		
		  if(from==0) 
		  { 
			  comments = new LinkedHashSet<>(); 
			  articles = new ArrayList<>();
			  Document doc = Jsoup.connect("https://slashdot.org/").get();
			  writeToFile(doc.toString(),"srap_article"); 
			  readSite(doc); 
		  } 
		  else 
		  { 
			  for(int i=from;i<to;i++) 
			  { 
				  comments = new LinkedHashSet<>(); 
				  articles = new ArrayList<>(); 
				  Document doc = Jsoup.connect("https://slashdot.org/?page="+i).get();
				  writeToFile(doc.toString(),"srap_article"); readSite(doc); 
			  } 
		  }
		 
		
	/*
	 * comments = new LinkedHashSet<>(); articles = new ArrayList<>(); ArticleDto
	 * article = readArticle(doc); articles.add(article);
	 * articleRepository.saveAll(articleMapper.articleDtoToEntity(articles));
	 * readComments(doc,article); for(CommentDto dto : comments) {
	 * System.out.println(dto);
	 * commentRepository.save(commentMapper.commentDtoToEntity(dto)); }
	 */

	}

	private void readSite(Document doc) throws IOException, ParseException
	{
		Elements articleList = doc.select("article");
		for(Element article : articleList)
		{
			try
			{
				if(article.hasAttr("data-fhtype") && article.attr("data-fhtype").equals("story"))
				{
					ArticleDto articleDb = readArticle(article);

					//articleDb = articleRepository.saveAndFlush(articleDb);

					Elements comments = article.getElementsByClass("comment-bubble");
					if(null != comments && !comments.isEmpty()) 
					{ 
						Elements comment = comments.get(0).select("a");
						if(null != comment && null != comment.get(0) 
								&& null != comment.get(0).text() && !comment.get(0).text().isEmpty())
						{
							System.out.println(comment.get(0).text());
							int count = Integer.parseInt(comment.get(0).text());
							articleDb.setCommentCount(count);

							Document commentDoc = Jsoup.connect("https:"+comment.attr("href")).get();
							writeToFile(commentDoc.toString(), "article_comm_");
							readComments(commentDoc,articleDb);
						}
						else
							articleDb.setCommentCount(0);
					}
					articles.add(articleDb);
				}
			}
			catch(Exception e)
			{
				System.out.println("error: "+e.getMessage());
			}

		}
		
		if(!articles.isEmpty())
		{
			articleRepository.saveAll(articleMapper.articleDtoToEntity(articles));
		}
		if(!comments.isEmpty())
		{
			for(CommentDto dto : comments)
			{
				System.out.println(dto);
				commentRepository.save(commentMapper.commentDtoToEntity(dto));
			}
			//commentRepository.saveAll(commentMapper.articleDtoToEntity(comments));
		}
		
	}
	
	private ArticleDto readArticle(Element article) throws IOException, ParseException
	{
		System.out.println("<------------------------------- Reading Article ------------------------------>");

		ArticleDto articleDb = new ArticleDto(); 
		Element articleId = article.getElementsByClass("sd-key-firehose-id").get(0);
		Element articleHead = article.select("header").get(0); 
		Element storyTitle = articleHead.getElementsByAttributeValue("class", "story-title").get(0);
		Elements storySources = articleHead.getElementsByAttributeValue("class","story-sourcelnk"); 
		System.out.println(storySources);
		if(null != storySources && !storySources.isEmpty())
			articleDb.setSource(storySources.get(0).attr("href"));
		Element storyPostedBy = articleHead.getElementsByAttributeValue("class", "story-byline").get(0);
		
		articleDb.setArticleId(Integer.parseInt(articleId.text()));

		System.out.println(storyTitle.children().get(0).text());
		articleDb.setTitle(storyTitle.children().get(0).text());
		
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
		
		return articleDb;
	}

	private void readComments(Document doc, ArticleDto article) throws IOException, ParseException
	{
		Element link = doc.getElementsByClass("commentBoxLinks").get(0);
		String cmntLink = null;
		if(null != link)
		{
			cmntLink = link.select("a").stream().filter(a -> a.attr("href").contains("sid="))
					.findFirst().get().attr("href");
			cmntLink = "https:"+cmntLink.split("&", 2)[0] + "&cid="; 
			System.out.println("Commentlink: "+cmntLink);
			article.setLink(cmntLink);
		}
		Element commentList = doc.getElementById("commentlisting");
		Elements parentComments = commentList.children();
		//writeToFile(comments.toString());
		for(Element c : parentComments)
			readComment(c, null, article);
			
	}
	
	private int readComment(Element c,CommentDto parrentCmmt, ArticleDto article) throws ParseException, IOException
	{
		int depth=0;
		CommentClass cmmtCls = CommentClass.getByDesc(c.select("li").get(0).className());
		if(null != cmmtCls)
		{
			int cmmtId = getCommentCode(c);

			CommentDto comment = new CommentDto();
			comment.setArticle(article);
			comment.setParentComment(parrentCmmt);
			comment.setCommentId(cmmtId);
			comment.setCls(cmmtCls);

			if(cmmtCls.equals(CommentClass.FULL_CONTAIN))
			{
				System.out.println("<------------------------------- Reading Full Comment "+cmmtId+" ------------------------------>");

				comments.add(comment);
				readCommentDetails(c,comment);
				Elements replies = getReplyCount(c, comment);
				depth = readCommentReplies(replies,comment,article)+1;
				comment.setDepth(depth);
				
			}
			else if(cmmtCls.equals(CommentClass.ONELINE))
			{
				System.out.println("<------------------------------- Reading Oneline Comment "+cmmtId+" ------------------------------>");
				comments.add(comment);
				commentLink = "https:"+getCommentLink(c,cmmtId);
				c = scrapCommentSection(commentLink, cmmtId);
				
				readCommentDetails(c,comment);
				Elements replies = getReplyCount(c, comment);
				depth = readCommentReplies(replies,comment,article)+1;
				comment.setDepth(depth);
			}
			else if(cmmtCls.equals(CommentClass.HIDDEN))
			{
				System.out.println("<------------------------------- Reading Hidden Comment "+cmmtId+" ------------------------------>");

				/*
				 * if(commentLink.isEmpty()) pendingComments.add(comment); else {
				 *  if(hiddenCommentLink.isEmpty()) { String linkParams[]
				 * = commentLink.split("cid="); hiddenCommentLink = linkParams[0]+"cid="; }
				 */
				comments.add(comment);
				String link = article.getLink()+comment.getCommentId();
				System.out.println(link);
				c = scrapCommentSection(link, cmmtId);
				writeToFile(c.toString(), "comment_"+comment.getCommentId());
				readCommentDetails(c,comment);
				Elements replies = getReplyCount(c, comment);
				depth = readCommentReplies(replies,comment,article)+1;
				comment.setDepth(depth);

			}
		}
		return depth;
	}
	
	private void readCommentDetails(Element c,CommentDto commentDb) throws ParseException
	{
		Element comment = c.getElementById("comment_"+commentDb.getCommentId());
		readCommentTitle(comment, commentDb);
		readCommentByAndTime(comment,commentDb);
		readCommentBody(comment,commentDb);
	}
	
	private void readCommentTitle(Element comment, CommentDto commentDb)
	{
		
		Elements temp = comment.getElementsByClass("title");
		if(null != temp)
		{
			Element cmmtTitle = temp.get(0);
			String title = cmmtTitle.getElementsByAttributeValue("id", "comment_link_"
					+commentDb.getCommentId()).get(0).text();
			System.out.println("title: "+title);
			commentDb.setTitle(title);
			
			String type = cmmtTitle.getElementsByAttributeValue("id", "comment_score_"
					+commentDb.getCommentId()).get(0).text();
			
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
	
	private void readCommentByAndTime(Element comment, CommentDto commentDb) throws ParseException
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
			
			if(null != commentDb.getParentComment())
			{
				long timeDiff = commentDb.getPostTime().getTime() - commentDb.getParentComment().getPostTime().getTime();
				commentDb.setTimeDiff(timeDiff/60000);
			}
			else
				commentDb.setTimeDiff(Long.valueOf(0));
		}
	}
	
	private void readCommentBody(Element comment, CommentDto commentDb)
	{
		String body = comment.getElementsByAttributeValue("id", "comment_body_"
				+commentDb.getCommentId()).get(0).text();
		System.out.println("body: "+body);
		commentDb.setContent(body);
	}
	
	private Document scrapCommentSection(String link, int id) throws IOException
	{
		System.out.println(link);
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
	
	private int readCommentReplies(Elements replies, CommentDto comment, ArticleDto article) throws ParseException, IOException 
	{	
		System.out.println("<------------------------------- Reading Replies ------------------------------>");

		int maxDepth=0;
		if(!(null == replies || replies.isEmpty()))
			for(Element r:replies)
				maxDepth = Math.max(maxDepth, readComment(r, comment, article));
		
		return maxDepth;
	}

	private Elements getReplyCount(Element c, CommentDto comment)
	{
		Elements replies = null;
		Element cmmtRlyTree = c.getElementsByAttributeValueMatching("id", "commtree_"+comment.getCommentId())
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
		  Pattern pattern = Pattern.compile("on (.*?)AM"); 
		  Matcher matcher = pattern.matcher(time); 
		  if (matcher.find()) {
			  //System.out.println(matcher.group(1));
			  time = matcher.group(1)+"AM";
		  }
		  else
		  {
			  Pattern pattern1 = Pattern.compile("on (.*?)PM"); 
			  Matcher matcher1 = pattern1.matcher(time); 
			  if (matcher1.find()) {
				  //System.out.println(matcher1.group(1)); 
				  time = matcher1.group(1)+"PM";
			  }
		  }
		 System.out.println(time);
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
