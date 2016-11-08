package com.journaldev.mongodb.servlets;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.journaldev.mongodb.dao.MongoDBPersonDAO;
import com.journaldev.mongodb.model.Person;
import com.mongodb.MongoClient;

@WebServlet("/editPerson")
public class EditPersonServlet extends HttpServlet {

	private static final long serialVersionUID = -6554920927964049383L;

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String id = request.getParameter("id");
		if (id == null || "".equals(id)) {
			throw new ServletException("id missing for edit operation");
		}
		System.out.println("Person edit requested with id=" + id);
		MongoClient mongo = (MongoClient) request.getServletContext()
				.getAttribute("MONGO_CLIENT");
		MongoDBPersonDAO personDAO = new MongoDBPersonDAO(mongo);
		Person p = new Person();
		p.setId(id);
		p = personDAO.readPerson(p);
		request.setAttribute("person", p);

		RequestDispatcher rd = getServletContext().getRequestDispatcher(
				"/editPerson.jsp");
		rd.forward(request, response);
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String id = request.getParameter("id"); // keep it non-editable in UI
		if (id == null || "".equals(id)) {
			throw new ServletException("id missing for edit operation");
		}

		String name = request.getParameter("name");
		String country = request.getParameter("country");

		if ((name == null || name.equals(""))
				|| (country == null || country.equals(""))) {
			request.setAttribute("error", "Name and Country Can't be empty");
			Person p = new Person();
			p.setId(id);
			p.setName(name);
			p.setCountry(country);
			request.setAttribute("person", p);

			RequestDispatcher rd = getServletContext().getRequestDispatcher(
					"/editPerson.jsp");
			rd.forward(request, response);
		} else {
			MongoClient mongo = (MongoClient) request.getServletContext()
					.getAttribute("MONGO_CLIENT");
			MongoDBPersonDAO personDAO = new MongoDBPersonDAO(mongo);
			Person p = new Person();
			p.setId(id);
			p.setName(name);
			p.setCountry(country);
			personDAO.updatePerson(p);
			request.setAttribute("person", p);
			System.out.println("Person edited successfully with id=" + id);
			request.setAttribute("success", "Person edited successfully");

			RequestDispatcher rd = getServletContext().getRequestDispatcher(
					"/searchPerson.jsp");
			rd.forward(request, response);
		}
	}

}