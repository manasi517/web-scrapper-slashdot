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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.transaction.Transactional;

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
		
		  if(from==0) 
		  { 
			  Document doc = Jsoup.connect("https://slashdot.org/").get();
			  readSite(doc); 
		  } 
		  else 
		  { 
			  for(int i=from;i<to;i++) 
			  {  
				  Document doc = Jsoup.connect("https://slashdot.org/?page="+i).get();
				  readSite(doc); 
			  } 
		  }
	}

	/**
	 * Method is used to parse the HTML file from Slashdot
	 * @param doc
	 * @throws IOException
	 * @throws ParseException
	 */
	private void readSite(Document doc) throws IOException, ParseException
	{
		Elements articleList = doc.select("article");
		for(Element article : articleList)
		{
			try
			{
				if(article.hasAttr("data-fhtype") && article.attr("data-fhtype").equals("story"))
				{
					comments = new LinkedHashSet<>(); 
					articles = new ArrayList<>();
					ArticleDto articleDb = readArticle(article);

					Elements commentsL = article.getElementsByClass("comment-bubble");
					if(null != commentsL && !commentsL.isEmpty()) 
					{ 
						Elements comment = commentsL.get(0).select("a");
						if(null != comment && null != comment.get(0) 
								&& null != comment.get(0).text() && !comment.get(0).text().isEmpty())
						{
							int count = Integer.parseInt(comment.get(0).text());
							articleDb.setCommentCount(count);

							Document commentDoc = Jsoup.connect("https:"+comment.attr("href")).get();
							readComments(commentDoc,articleDb);
						}
						else
							articleDb.setCommentCount(0);
					}
					
					articles.add(articleDb);
					databaseOps();
					articles.clear();
					comments.clear();
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

		}
		
	}
	
	/**
	 * Method is used to save all the extracted data from article and comments
	 */
	@Transactional()
	private void databaseOps()
	{
		articleRepository.saveAll(articleMapper.articleDtoToEntity(articles));
		
		for(CommentDto dto : comments)
			commentRepository.save(commentMapper.commentDtoToEntity(dto));
		
	}
	
	/**
	 * Method is used to extract the Article data from each article tag on the page
	 * @param article
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	private ArticleDto readArticle(Element article) throws IOException, ParseException
	{

		ArticleDto articleDb = new ArticleDto(); 
		Element articleId = article.getElementsByClass("sd-key-firehose-id").get(0);
		Element articleHead = article.select("header").get(0); 
		Element storyTitle = articleHead.getElementsByAttributeValue("class", "story-title").get(0);
		Elements storySources = articleHead.getElementsByAttributeValue("class","story-sourcelnk"); 
		
		if(null != storySources && !storySources.isEmpty())
			articleDb.setSource(storySources.get(0).attr("href"));
		Element storyPostedBy = articleHead.getElementsByAttributeValue("class", "story-byline").get(0);
		
		articleDb.setArticleId(Integer.parseInt(articleId.text()));

		articleDb.setTitle(storyTitle.children().get(0).text());
		
		Element dept = storyPostedBy.getElementsByClass("dept-text").first();
		articleDb.setFromDept(dept.text());
		dept.remove();
		
		Element time = storyPostedBy.select("time").get(0);
		Date date = parseDate(time.text());
		articleDb.setPostTime(date);
		time.remove();
		
		String by= storyPostedBy.ownText().replace("Posted by", "").replace("from the", "")
				.replace("dept.", "").trim();
		articleDb.setPostedBy(by);
		
		articleDb.setContent(article.getElementsByClass("body").text());
		
		return articleDb;
	}

	/**
	 * Method is used to read the comment section associated with an Article and iterate through each direct comment
	 * @param doc
	 * @param article
	 * @throws Exception
	 */
	private void readComments(Document doc, ArticleDto article) throws Exception
	{
		Element link = doc.getElementsByClass("commentBoxLinks").get(0);
		Optional<Element> cmntLink = null;
		if(null != link)
		{
			cmntLink = link.select("a").stream().filter(a -> a.attr("href").contains("sid="))
					.findFirst();
			if(!cmntLink.isPresent())
			{
				if(article.getCommentCount()>0)
					throw new Exception("Invalid comments link");
				return;
			}
			String commentLink = cmntLink.get().attr("href");
			commentLink = "https:"+commentLink.split("&", 2)[0] + "&cid="; 
			article.setLink(commentLink);
		}
		
		Element commentList = doc.getElementById("commentlisting");
		Elements parentComments = commentList.children();
		for(Element c : parentComments)
			readComment(c, null, article);
			
	}
	
	/**
	 * Method is used to extract data from each comment
	 * @param c
	 * @param parrentCmmt
	 * @param article
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
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
				//System.out.println("<------------------------------- Reading Full Comment "+cmmtId+" ------------------------------>");

				comments.add(comment);
				readCommentDetails(c,comment);
				Elements replies = getReplyCount(c, comment);
				depth = readCommentReplies(replies,comment,article)+1;
				comment.setDepth(depth);
				
			}
			else if(cmmtCls.equals(CommentClass.ONELINE))
			{
				//System.out.println("<------------------------------- Reading Oneline Comment "+cmmtId+" ------------------------------>");
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
				//System.out.println("<------------------------------- Reading Hidden Comment "+cmmtId+" ------------------------------>");

				comments.add(comment);
				String link = article.getLink()+comment.getCommentId();
				c = scrapCommentSection(link, cmmtId);
				readCommentDetails(c,comment);
				Elements replies = getReplyCount(c, comment);
				depth = readCommentReplies(replies,comment,article)+1;
				comment.setDepth(depth);

			}
		}
		return depth;
	}
	
	/**
	 * Method is used to extract the details of each comment
	 * @param c
	 * @param commentDb
	 * @throws ParseException
	 */
	private void readCommentDetails(Element c,CommentDto commentDb) throws ParseException
	{
		Element comment = c.getElementById("comment_"+commentDb.getCommentId());
		readCommentTitle(comment, commentDb);
		readCommentByAndTime(comment,commentDb);
		readCommentBody(comment,commentDb);
	}
	
	/**
	 * Method is used to extract title and score of the comment
	 * @param comment
	 * @param commentDb
	 */
	private void readCommentTitle(Element comment, CommentDto commentDb)
	{
		
		Elements temp = comment.getElementsByClass("title");
		if(null != temp)
		{
			Element cmmtTitle = temp.get(0);
			String title = cmmtTitle.getElementsByAttributeValue("id", "comment_link_"
					+commentDb.getCommentId()).get(0).text();
			//System.out.println("title: "+title);
			commentDb.setTitle(title);
			
			String type = cmmtTitle.getElementsByAttributeValue("id", "comment_score_"
					+commentDb.getCommentId()).get(0).text();
			
			String[] scores = type.replace("(", "").replace(")", "").trim().split(",");
			//System.out.println("score: "+scores[0].replace("Score:", ""));
			commentDb.setScore(Integer.parseInt(scores[0].replace("Score:", "")));
			
			if(scores.length>1)
			{
				commentDb.setType(scores[1].trim());
				//System.out.println("type: "+scores[1].trim());
			}
		}
	}
	
	/**
	 * Method is used to extract posted by and post time of the comment
	 * @param comment
	 * @param commentDb
	 * @throws ParseException
	 */
	private void readCommentByAndTime(Element comment, CommentDto commentDb) throws ParseException
	{
		Elements temp = comment.getElementsByClass("details");
		if(null != temp)
		{
			Element cmmtBy = temp.get(0);
			String by = cmmtBy.getElementsByClass("by").get(0).text().replace("by", "").trim();
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
	
	/**
	 * Method is used to extract main content of the comment
	 * @param comment
	 * @param commentDb
	 */
	private void readCommentBody(Element comment, CommentDto commentDb)
	{
		String body = comment.getElementsByAttributeValue("id", "comment_body_"
				+commentDb.getCommentId()).get(0).text();
		//System.out.println("body: "+body);
		commentDb.setContent(body);
	}
	
	private Document scrapCommentSection(String link, int id) throws IOException
	{
		Document doc = Jsoup.connect(link).get();
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
	
	/**
	 * Method is used to read and iterate over each reply for a comment
	 * @param replies
	 * @param comment
	 * @param article
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	private int readCommentReplies(Elements replies, CommentDto comment, ArticleDto article) throws ParseException, IOException 
	{	
		//System.out.println("<------------------------------- Reading Replies ------------------------------>");

		//depth parameter would be decided here, while backtracking from the replies
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
		return Integer.parseInt(cmmtId);
	}
	
	/**
	 * Method is used to read and format the post time according to different structures of time mentioned on Slashdot
	 * @param time
	 * @return
	 * @throws ParseException
	 */
	private Date parseDate(String time) throws ParseException
	{	
		  Pattern pattern = Pattern.compile("on (.*?)AM"); 
		  Matcher matcher = pattern.matcher(time); 
		  if (matcher.find()) {
			  time = matcher.group(1)+"AM";
		  }
		  else
		  {
			  Pattern pattern1 = Pattern.compile("on (.*?)PM"); 
			  Matcher matcher1 = pattern1.matcher(time); 
			  if (matcher1.find()) { 
				  time = matcher1.group(1)+"PM";
			  }
		  }

		DateFormat format = new SimpleDateFormat("EEEE MMMM dd, yyyy hh:mmaaa", Locale.ENGLISH);
		time = time.replaceFirst("@", "").trim();
		Date date = format.parse(time);
		return date;
	}
	
	/**
	 * Method is used to write the HTML file for debugging purpose
	 * @param s
	 * @param fileName
	 * @throws IOException
	 */
	private void writeToFile(String s, String fileName) throws IOException
	{
		
		 File yourFile = new File(fileName+".txt"); 
		 yourFile.createNewFile();
		 BufferedWriter writer = new BufferedWriter(new FileWriter(yourFile));
		 writer.write(s); 
		 writer.close();
		 
	}
}
