package com.testehan.springai.mongoatlas.controller;

import com.testehan.springai.mongoatlas.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.spring6.context.webflux.IReactiveDataDriverContextVariable;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;

@Controller
@RequestMapping("/")
public class IndexController {
    @Autowired
    private MovieService movieService;

    @GetMapping
    public String index() {
        return "index";
    }


    @GetMapping("/movies/semantic-search2")
    public String performSemanticSearch2(@RequestParam("plotDescription") String plotDescription,
                                         Model model) throws InterruptedException
    {
        IReactiveDataDriverContextVariable reactiveDataDrivenMode =
                new ReactiveDataDriverContextVariable( movieService.getMoviesSemanticSearch2(plotDescription), 1);

        model.addAttribute("items", reactiveDataDrivenMode);
        return "fragments/items";

//        ReactiveDataDriverContextVariable dataVariable =
//                new ReactiveDataDriverContextVariable(movieService.getMoviesSemanticSearch2(plotDescription), 1);
//        model.addAttribute("items", dataVariable);
//        return Mono.just("fragments/items");

//        var movies = movieService.getMoviesSemanticSearch2(plotDescription);
//        return Mono.just(Rendering.view("fragments/items")
//                .modelAttribute("items",movies ).build());


    }



}
