/*
 *  Copyright (c) 2017 Kamesh Sampath<kamesh.sampath@hotmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.workspace7.moviestore.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.workspace7.moviestore.data.Movie;
import org.workspace7.moviestore.data.MovieCart;
import org.workspace7.moviestore.data.MovieCartItem;
import org.workspace7.moviestore.utils.MovieDBHelper;

/**
 * @author kameshs
 */
@Controller
@Slf4j
public class HomeController {


    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    final MovieDBHelper movieDBHelper;

    @Autowired
    public HomeController(MovieDBHelper movieDBHelper) {
        this.movieDBHelper = movieDBHelper;
    }

    @GetMapping("/")
    public ModelAndView home(ModelAndView modelAndView) {

        final String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
        log.info("Request served by HOST {} ", hostname);

        List<Movie> movies = movieDBHelper.getAll();

        List<MovieCartItem> movieList = movies.stream()
            .map((Movie movie) -> MovieCartItem.builder()
                .movie(movie)
                .quantity(1)
                .total(0)
                .build())
            .collect(Collectors.toList());

        final HashOperations<String, Object, Object> hashOperations = this.redisTemplate.opsForHash();
        final Object storedCart = hashOperations.get(ShoppingCartController.SESSION_ATTR_MOVIE_CART,
            ShoppingCartController.SESSION_ATTR_MOVIE_CART);

        if (storedCart != null) {

            int cartCount = 0;
                MovieCart movieCart = (MovieCart) storedCart;

                if (movieCart != null) {

                    final Map<String, Integer> movieItems = movieCart.getMovieItems();

                    movieList = movieList.stream()
                        .map(movieCartItem -> {
                            Movie movie = movieCartItem.getMovie();
                            String movieId = movie.getId();
                            if (movieItems.containsKey(movieId)) {
                                int quantity = movieItems.get(movieId);
                                movieCartItem.setQuantity(quantity);
                            } else {
                                movieCartItem.setQuantity(1);
                            }
                            return movieCartItem;

                        }).collect(Collectors.toList());

                    cartCount = movieItems.size();
                }
            modelAndView.addObject("cartCount", cartCount);
            modelAndView.addObject("movies", movieList);
        } else {
            log.info("New Session");
            modelAndView.addObject("movies", movieList);
        }
        modelAndView.setViewName("home");
        modelAndView.addObject("hostname", hostname);
        return modelAndView;
    }


    @PostMapping("/logout")
    public ModelAndView clear(ModelAndView modelAndView) {
        final String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
        List<Movie> movies = movieDBHelper.getAll();

        List<MovieCartItem> movieList = movies.stream()
            .map((Movie movie) -> MovieCartItem.builder()
                .movie(movie)
                .quantity(0)
                .total(0)
                .build())
            .collect(Collectors.toList());


        final HashOperations<String, Object, Object> hashOperations = this.redisTemplate.opsForHash();
        hashOperations.delete(ShoppingCartController.SESSION_ATTR_MOVIE_CART, ShoppingCartController.SESSION_ATTR_MOVIE_CART);

        log.info("New Session");
        modelAndView.addObject("movies", movieList);
        modelAndView.setViewName("home");
        modelAndView.addObject("hostname", hostname);
        return modelAndView;
    }

    @GetMapping("/healthz")
    public ResponseEntity healthz() {
        return new ResponseEntity(HttpStatus.OK);
    }

}
