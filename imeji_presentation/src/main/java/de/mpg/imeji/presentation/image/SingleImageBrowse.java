/**
 * License: src/main/resources/license/escidoc.license
 */

package de.mpg.imeji.presentation.image;

import java.net.URI;

import javax.faces.context.FacesContext;

import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.util.BeanHelper;


public class SingleImageBrowse 
{
	private ImagesBean imagesBean = null;
	private Item currentImage = null;

	private String next = null;
	private String previous = null;

	public SingleImageBrowse(ImagesBean imagesBean, Item item, String type,String path) 
	{
		this.imagesBean = imagesBean;
		currentImage = item;
		init(type,path);
	}

	public void init(String type,String path)
	{
		String baseUrl=new String();
		if(type=="collection"){
			baseUrl= ((Navigation)BeanHelper.getApplicationBean(Navigation.class)).getCollectionUrl()+path+"/item/";
		}
		else if(type=="item"){
			baseUrl= ((Navigation)BeanHelper.getApplicationBean(Navigation.class)).getItemUrl();
		}
		else if(type=="album"){
			baseUrl= ((Navigation)BeanHelper.getApplicationBean(Navigation.class)).getAlbumUrl()+path+"/item/";
		}
		URI nextImage = getNextImageFromList();
		URI prevImage = getPreviousImageFromList();

		String direction = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("nav");

		if (nextImage == null && prevImage == null)
		{
			if ("prev".equals(direction))
			{
				loadPreviousPage();
			}
			else if ("next".equals(direction))
			{
				loadNextPage();
			}
			nextImage = getNextImageFromList();
			prevImage = getPreviousImageFromList();
		}

		if (nextImage == null && loadNextPage())
		{
			nextImage = getFirstImageOfPage();
			loadPreviousPage();
		}

		if (prevImage == null && loadPreviousPage()) 
		{
			prevImage = getLastImageOfPage();
			loadNextPage();
		}

		if (nextImage != null)
		{
			next = baseUrl + ObjectHelper.getId(nextImage) + "?nav=next";
		}

		if (prevImage != null)
		{
			previous = baseUrl +  ObjectHelper.getId(prevImage)+ "?nav=prev";
		}
	}

	public URI getNextImageFromList()
	{
		for(int i=0; i < imagesBean.getCurrentPartList().size() - 1 ; i++)
		{
			if (imagesBean.getCurrentPartList().get(i).getUri().equals(currentImage.getId()))
			{
				return imagesBean.getCurrentPartList().get(i + 1).getUri();
			}
		}
		return null;
	}

	public URI getPreviousImageFromList()
	{
		for(int i= 1; i < imagesBean.getCurrentPartList().size() ; i++)
		{
			if (imagesBean.getCurrentPartList().get(i).getUri().equals(currentImage.getId()))
			{
				return imagesBean.getCurrentPartList().get(i - 1).getUri();
			}
		}
		return null;
	}

	public URI getFirstImageOfPage()
	{
		if (imagesBean.getCurrentPartList().size() > 0)
		{
			return imagesBean.getCurrentPartList().get(0).getUri();
		}
		return null;
	}

	public URI getLastImageOfPage()
	{
		if (imagesBean.getCurrentPartList().size() > 0) 
		{
			return imagesBean.getCurrentPartList().get(imagesBean.getCurrentPartList().size() - 1).getUri();
		}
		return null;
	}

	public boolean loadNextPage()
	{
		if (imagesBean.getCurrentPageNumber() < imagesBean.getPaginatorPageSize())
		{
			imagesBean.goToNextPage();
			imagesBean.update();
			return true;
		}
		return false;
	}

	public boolean loadPreviousPage()
	{
		if(imagesBean.getCurrentPageNumber() > 1)
		{
			imagesBean.goToPreviousPage();
			imagesBean.update();
			return true;
		}
		return false;
	}

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}

	public String getPrevious() {
		return previous;
	}

	public void setPrevious(String previous) {
		this.previous = previous;
	}
}
