package com.vision.app

import android.view.accessibility.AccessibilityNodeInfo
object ScreenAnalyzer {
 data class UIElement(val text:String,val resourceId:String,val className:String,val isClickable:Boolean,val isVisible:Boolean,val bounds:String,val contentDescription:String="",val hint:String="")
 fun analyzeScreenTree(root:AccessibilityNodeInfo?):String{if(root==null)return "Screen not accessible";val e=mutableListOf<UIElement>();traverse(root,e);val out=StringBuilder();out.append("App: ").append(root.packageName).append("\nScreen Elements:\n");e.filter{it.isClickable}.forEach{out.append("Button: ").append(it.text).append(" ").append(it.contentDescription).append(" @").append(it.bounds).append("\n")};e.filter{!it.isClickable&&it.text.isNotBlank()}.take(30).forEach{out.append("Text: ").append(it.text).append("\n")};return out.toString()}
 private fun traverse(n:AccessibilityNodeInfo,e:MutableList<UIElement>){if(n.isVisibleToUser)e.add(UIElement(n.text?.toString()?:(""),n.viewIdResourceName?:(""),n.className?.toString()?:(""),n.isClickable,true,n.boundsInScreen.run{"$left,$top,$right,$bottom"},n.contentDescription?.toString()?:("")));for(i in 0 until n.childCount)n.getChild(i)?.let{traverse(it,e);it.recycle()}}
 fun findButtonByText(root:AccessibilityNodeInfo?,target:String):AccessibilityNodeInfo?{if(root==null)return null;if(root.isClickable&&((root.text?.contains(target,true)==true)||(root.contentDescription?.contains(target,true)==true)))return root;for(i in 0 until root.childCount)root.getChild(i)?.let{val r=findButtonByText(it,target);if(r!=null)return r;it.recycle()};return null}
 fun findElementById(root:AccessibilityNodeInfo?,id:String):AccessibilityNodeInfo?{if(root==null)return null;if(root.viewIdResourceName==id)return root;for(i in 0 until root.childCount)root.getChild(i)?.let{val r=findElementById(it,id);if(r!=null)return r;it.recycle()};return null}
 fun getAllClickableElements(root:AccessibilityNodeInfo?):List<UIElement>{val e=mutableListOf<UIElement>();if(root!=null)traverse(root,e);return e.filter{it.isClickable}}
}
