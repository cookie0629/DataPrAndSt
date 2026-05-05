const { celebrate, Joi } = require('celebrate');

const isFlightSearchQuery = celebrate({
  query: Joi.object().keys({
    origin: Joi.string().required(),
    destination: Joi.string().required(),
    departureDate: Joi.string()
      .pattern(/^\d{4}-\d{2}-\d{2}$/)
      .required(),
    fareConditions: Joi.string().valid('Economy', 'Comfort', 'Business'),
    transfers: Joi.number().integer().min(0),
  }),
});

const isCheckinRequest = celebrate({
  body: Joi.object().keys({
    flightId: Joi.number().integer().required(),
    ticketNo: Joi.string().length(13).required(),
  }),
});

module.exports = { isFlightSearchQuery, isCheckinRequest };
